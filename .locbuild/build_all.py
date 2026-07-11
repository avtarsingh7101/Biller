# Build the offline asset from GeoNames India IN.zip — ALL feature classes.
import zipfile, os
BASE = r"C:\Users\AVTAR Singh\CabBillingApp"
LOC = os.path.join(BASE, ".locbuild")
OUT = os.path.join(BASE, "app", "src", "main", "assets", "in_cities.txt")
admin = {}
with open(os.path.join(LOC, "admin1.txt"), encoding="utf-8") as f:
    for line in f:
        r = line.rstrip("\n").split("\t")
        if len(r) >= 2 and r[0].startswith("IN."):
            admin[r[0]] = r[1]
seen, out = set(), []
with zipfile.ZipFile(os.path.join(LOC, "IN.zip")) as z:
    with z.open("IN.txt") as raw:
        for b in raw:
            r = b.decode("utf-8", "replace").rstrip("\n").split("\t")
            if len(r) < 15:
                continue
            name = (r[2] or r[1]).strip()          # asciiname preferred
            if not name or "|" in name:
                continue
            state = admin.get("IN." + (r[10] or ""), "").strip()
            k = (name.lower(), state.lower())
            if k in seen:
                continue
            seen.add(k)
            out.append(name + "|" + state)
out.sort(key=lambda s: (s.split("|")[0].lower(), s.lower()))
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    for e in out:
        f.write(e + "\n")
print("entries:", len(out), "bytes:", os.path.getsize(OUT))
