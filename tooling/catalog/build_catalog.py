#!/usr/bin/env python3
"""Build Weekplate's public, static price snapshot from grocery-cli JSON."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import pathlib
import subprocess
import sys
from typing import Any


STORE_LABELS = {
    "mercadona": "Mercadona",
    "dia": "DIA",
    "lidl-es": "Lidl España",
    "alcampo": "Alcampo",
    "consum": "Consum",
    "eroski": "Eroski",
    "bonpreu": "Bonpreu i Esclat",
    "ametller": "Ametller Origen",
    "gadis": "Gadis",
    "aldi": "ALDI España",
    "makro": "Makro España",
    "masymas": "masymas",
}


def read_queries(path: pathlib.Path) -> list[str]:
    values: list[str] = []
    seen: set[str] = set()
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        value = raw_line.split("#", 1)[0].strip()
        key = value.casefold()
        if value and key not in seen:
            seen.add(key)
            values.append(value)
    if not values:
        raise ValueError("the query file is empty")
    return values


def clean_product(value: Any) -> dict[str, Any] | None:
    if not isinstance(value, dict):
        return None
    product_id = str(value.get("id", "")).strip()
    name = str(value.get("name", "")).strip()
    try:
        price = float(value.get("price", 0))
    except (TypeError, ValueError):
        return None
    if not product_id or not name or not 0 < price <= 100_000:
        return None

    result: dict[str, Any] = {
        "id": product_id,
        "name": name,
        "price": round(price, 4),
        "currency": str(value.get("currency") or "EUR"),
        "available": bool(value.get("available", True)),
    }
    for key in ("unit", "brand", "category", "url"):
        if value.get(key):
            result[key] = str(value[key]).strip()
    try:
        unit_price = float(value.get("pricePerUnit", 0))
        if unit_price > 0:
            result["pricePerUnit"] = round(unit_price, 4)
    except (TypeError, ValueError):
        pass
    return result


def run_store(grocery: str, store: str, queries: list[str], timeout: int) -> dict[str, Any]:
    command = [
        grocery,
        "--store",
        store,
        "--lang",
        "es",
        "--json",
        "batch",
        "-f",
        "-",
    ]
    try:
        completed = subprocess.run(
            command,
            input="\n".join(queries) + "\n",
            text=True,
            capture_output=True,
            timeout=timeout,
            check=False,
        )
        rows = json.loads(completed.stdout)
        if not isinstance(rows, list):
            raise ValueError("batch output is not a JSON array")
        items = []
        for row in rows:
            if not isinstance(row, dict):
                continue
            term = str(row.get("term", "")).strip()
            if term:
                items.append({"term": term, "product": clean_product(row.get("product"))})
        found = sum(item["product"] is not None for item in items)
        status = "ok" if found == len(queries) else "partial"
        output: dict[str, Any] = {
            "id": store,
            "name": STORE_LABELS[store],
            "currency": "EUR",
            "status": status,
            "items": items,
        }
        if completed.returncode != 0:
            output["error"] = compact_error(completed.stderr or f"exit {completed.returncode}")
        return output
    except (OSError, subprocess.SubprocessError, ValueError, json.JSONDecodeError) as error:
        return {
            "id": store,
            "name": STORE_LABELS[store],
            "currency": "EUR",
            "status": "error",
            "error": compact_error(str(error)),
            "items": [],
        }


def compact_error(value: str) -> str:
    return " ".join(value.split())[-500:] or "unknown error"


def grocery_version(grocery: str) -> str | None:
    try:
        result = subprocess.run(
            [grocery, "version"], text=True, capture_output=True, timeout=20, check=False
        )
        value = (result.stdout or result.stderr).strip()
        return value[:120] or None
    except (OSError, subprocess.SubprocessError):
        return None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--grocery", default="grocery")
    parser.add_argument("--queries", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--stores", default=",".join(STORE_LABELS))
    parser.add_argument("--timeout", type=int, default=180)
    args = parser.parse_args()

    stores = [value.strip() for value in args.stores.split(",") if value.strip()]
    unknown = sorted(set(stores) - STORE_LABELS.keys())
    if unknown:
        parser.error(f"unsupported store keys: {', '.join(unknown)}")
    queries = read_queries(args.queries)

    store_results = []
    for store in stores:
        print(f"Fetching {store} ({len(queries)} terms)", file=sys.stderr, flush=True)
        store_results.append(run_store(args.grocery, store, queries, args.timeout))

    if not any(store["items"] for store in store_results):
        print("No store returned parseable items; refusing to publish an empty feed", file=sys.stderr)
        return 2

    generated_at = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")
    payload = {
        "schemaVersion": 1,
        "generatedAt": generated_at,
        "source": {
            "name": "grocery-cli",
            "version": grocery_version(args.grocery),
            "repository": "https://github.com/jgalea/grocery-cli",
        },
        "stores": store_results,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    temporary.replace(args.output)
    found = sum(sum(item["product"] is not None for item in store["items"]) for store in store_results)
    print(f"Wrote {args.output} with {found} matched prices", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
