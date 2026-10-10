"""Generates item texture + mod icon with only the standard library (zlib/struct)."""
import struct, zlib, math, os

def png(path, w, h, px):
    raw = b''.join(b'\x00' + bytes([c for p in px[y*w:(y+1)*w] for c in p]) for y in range(h))
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(data)

def darkhold():
    T = (0, 0, 0, 0)
    g = [[T] * 16 for _ in range(16)]
    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16: g[y][x] = c + (255,) if len(c) == 3 else c
    OUT = (14, 6, 12); COVER = (74, 24, 38); COVER_D = (52, 16, 28); COVER_L = (104, 38, 52)
    SPINE = (36, 10, 20); PAGE = (214, 196, 160); PAGE_D = (170, 150, 118)
    GOLD = (214, 170, 66); GOLD_D = (150, 110, 36)
    EYE = (255, 54, 84); EYE_C = (255, 190, 90); EYE_G = (190, 24, 60)
    # book body x 2..13, y 1..14
    for y in range(1, 15):
        for x in range(2, 14):
            put(x, y, COVER)
    for y in range(1, 15):
        put(2, y, OUT); put(13, y, OUT)
        put(3, y, SPINE); put(4, y, SPINE)
    for x in range(2, 14):
        put(x, 1, OUT); put(x, 14, OUT)
    # lighter top-right bevel and darker bottom edge
    for x in range(5, 13): put(x, 2, COVER_L)
    for y in range(2, 14): put(12, y, COVER_D)
    for x in range(5, 13): put(x, 13, COVER_D)
    # page block visible at bottom
    for x in range(5, 13): put(x, 14, PAGE)
    for x in range(5, 13): put(x, 13, PAGE_D)
    put(13, 14, OUT); put(13, 13, OUT)
    # gold corners + spine bands
    for (x, y) in [(5, 2), (6, 2), (5, 3), (11, 2), (12, 2), (12, 3), (5, 12), (6, 12), (5, 11), (11, 12), (12, 12), (12, 11)]:
        put(x, y, GOLD)
    for y in (3, 7, 11):
        put(3, y, GOLD_D); put(4, y, GOLD)
    # glowing eye sigil in the centre (x 6..11, y 5..10)
    for x, y in [(6, 7), (7, 6), (8, 6), (9, 6), (10, 7), (7, 8), (8, 9), (9, 9), (10, 8), (6, 8)]:
        put(x, y, EYE_G)
    for x, y in [(7, 7), (8, 7), (9, 7), (8, 8), (9, 8)]:
        put(x, y, EYE)
    put(8, 8, EYE_C)
    put(8, 5, EYE_G); put(8, 10, EYE_G); put(5, 8, (150, 20, 50))
    # soft outer glow dots
    for x, y in [(8, 4), (7, 5), (9, 5)]:
        put(x, y, (130, 30, 50))
    return [c for row in g for c in row]

def icon(size=128):
    px = []
    cols = [(228, 18, 45), (62, 155, 255), (168, 69, 255)]
    centers = [(0.36, 0.40), (0.64, 0.40), (0.50, 0.66)]
    for y in range(size):
        for x in range(size):
            u, v = x / size, y / size
            r = g = b = 0.0
            for (cx, cy), (cr, cg, cb) in zip(centers, cols):
                d = math.hypot(u - cx, v - cy)
                k = max(0.0, 1.0 - d / 0.34) ** 1.6
                r += cr * k; g += cg * k; b += cb * k
            bg = 14 + 10 * (1 - v)
            r = min(255, r + bg); g = min(255, g + bg); b = min(255, b + bg + 8)
            d0 = math.hypot(u - .5, v - .5)
            a = 255
            border = abs(u - .5) > .47 or abs(v - .5) > .47
            if border: r, g, b = r * .5, g * .5, b * .5
            px.append((int(r), int(g), int(b), a))
    return px

base = os.path.dirname(os.path.abspath(__file__)) + '/../src/main/resources/assets/hexpowers'
png(base + '/textures/item/darkhold.png', 16, 16, darkhold())
png(base + '/icon.png', 128, 128, icon())

# enlarged previews (nearest-neighbour) for eyeballing
def up(px, w, k):
    out = []
    for y in range(w):
        row = [px[y*w+x] for x in range(w) for _ in range(k)]
        out += row * k
    return out
d = darkhold()
bg = [(30, 30, 38, 255)] * 256
comp = [p if p[3] else b for p, b in zip(d, bg)]
png('/tmp/darkhold_preview.png', 16 * 16, 16 * 16, up(comp, 16, 16))
print("assets written")
