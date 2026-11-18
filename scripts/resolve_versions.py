#!/usr/bin/env python3
"""Pobiera aktualne, rzeczywiste wersje Fabric dla Minecrafta z gradle.properties i wpisuje je do pliku."""
import json, re, sys, urllib.request
import xml.etree.ElementTree as ET

PROPS = "gradle.properties"

def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return r.read().decode()

def maven_versions(url):
    root = ET.fromstring(get(url))
    return [v.text for v in root.findall("./versioning/versions/version")]

def read_props():
    d = {}
    for line in open(PROPS, encoding="utf-8"):
        if "=" in line and not line.lstrip().startswith("#"):
            k, v = line.split("=", 1); d[k.strip()] = v.strip()
    return d

def write_props(updates):
    out = []
    seen = set()
    for line in open(PROPS, encoding="utf-8").read().splitlines():
        k = line.split("=", 1)[0].strip() if "=" in line and not line.lstrip().startswith("#") else None
        if k in updates:
            out.append(f"{k}={updates[k]}"); seen.add(k)
        else:
            out.append(line)
    for k, v in updates.items():
        if k not in seen: out.append(f"{k}={v}")
    open(PROPS, "w", encoding="utf-8").write("\n".join(out) + "\n")

mc = read_props()["minecraft_version"]

yarn = json.loads(get(f"https://meta.fabricmc.net/v2/versions/yarn/{mc}"))
if not yarn:
    sys.exit(f"BLAD: brak map Yarn dla Minecrafta {mc}")
yarn_v = yarn[0]["version"]

loaders = json.loads(get("https://meta.fabricmc.net/v2/versions/loader"))
loader_v = next(l["version"] for l in loaders if l.get("stable"))

api = [v for v in maven_versions("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml")
       if v.endswith("+" + mc)]
if not api:
    sys.exit(f"BLAD: brak Fabric API dla Minecrafta {mc}")
api_v = api[-1]

loom = [v for v in maven_versions("https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml")
        if re.fullmatch(r"\d+\.\d+(\.\d+)?", v)]
if not loom:
    sys.exit("BLAD: nie znaleziono stabilnej wersji Fabric Loom")
key = lambda v: [int(x) for x in v.split(".")]
loom_v = sorted(loom, key=key)[-1]

upd = {"yarn_mappings": yarn_v, "loader_version": loader_v, "fabric_version": api_v, "loom_version": loom_v}
write_props(upd)
print("Wybrane wersje:"); [print(f"  {k} = {v}") for k, v in upd.items()]
