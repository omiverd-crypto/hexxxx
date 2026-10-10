"""Procedurally paints all magic FX textures (white + alpha = intensity; tinted per path in-game)."""
import math, os, random
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, '..', 'src/main/resources/assets/hexpowers/textures/fx'))
os.makedirs(OUT, exist_ok=True)
TAU = math.tau


def save(name, a, size_note=''):
    a = np.clip(a, 0, 1)
    rgba = np.zeros(a.shape + (4,), np.uint8)
    rgba[..., :3] = 255
    rgba[..., 3] = (a * 255 + 0.5).astype(np.uint8)
    Image.fromarray(rgba, 'RGBA').save(os.path.join(OUT, name + '.png'), optimize=True)
    print('wrote', name, a.shape[::-1])


def glowify(sharp, layers=((2.2, 0.85), (7.0, 0.55), (20.0, 0.26))):
    out = sharp.copy()
    for s, k in layers:
        out += gaussian_filter(sharp, s) * k
    return out


class Canvas:
    """Supersampled line-art canvas; coordinates are in output pixels relative to the centre."""
    def __init__(self, size=512, ss=4):
        self.n, self.ss, self.S = size, ss, size * ss
        self.im = Image.new('L', (self.S, self.S), 0)
        self.d = ImageDraw.Draw(self.im)
        self.c = size / 2

    def pt(self, x, y):
        return ((self.c + x) * self.ss, (self.c + y) * self.ss)

    def polar(self, r, a):
        return self.pt(r * math.cos(a), r * math.sin(a))

    def line(self, pts, w=2.5, v=255):
        P = [self.pt(*p) for p in pts]
        self.d.line(P, fill=v, width=max(1, int(w * self.ss)), joint='curve')
        r = w * self.ss / 2
        for p in (P[0], P[-1]):
            self.d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=v)

    def circle(self, r, w=2.5, x=0, y=0, v=255):
        s = self.ss
        self.d.ellipse([(self.c + x - r) * s, (self.c + y - r) * s, (self.c + x + r) * s, (self.c + y + r) * s],
                       outline=v, width=max(1, int(w * s)))

    def disc(self, r, x=0, y=0, v=255):
        s = self.ss
        self.d.ellipse([(self.c + x - r) * s, (self.c + y - r) * s, (self.c + x + r) * s, (self.c + y + r) * s], fill=v)

    def array(self):
        return np.asarray(self.im.resize((self.n, self.n), Image.LANCZOS), float) / 255.0


def polar_pts(r, angles):
    return [(r * math.cos(a), r * math.sin(a)) for a in angles]


def rings_and_ticks(cv, r_out=240, major=15, minor=3):
    cv.circle(r_out, 3.2)
    cv.circle(r_out - 9, 1.4)
    for i in range(360 // minor):
        a = math.radians(i * minor)
        big = (i * minor) % major == 0
        r0 = r_out - (16 if big else 12)
        cv.line([(r0 * math.cos(a), r0 * math.sin(a)), ((r_out - 9) * math.cos(a), (r_out - 9) * math.sin(a))], 1.6 if big else 1.0)


def dashed_ring(cv, r, n=72, frac=0.55, w=2.0):
    for i in range(n):
        a0 = TAU * i / n
        a1 = a0 + TAU / n * frac
        cv.line(polar_pts(r, [a0 + (a1 - a0) * t / 6 for t in range(7)]), w)


# ------------------------------------------------------------------ sigils
def sigil_chaos():
    cv = Canvas()
    rings_and_ticks(cv)
    cv.circle(204, 2.6)
    dashed_ring(cv, 214, 90, 0.5, 1.6)
    cv.circle(196, 1.2)
    # eight-arrow chaos star
    for k in range(8):
        a = k * math.pi / 4
        dx, dy = math.cos(a), math.sin(a)
        px, py = -dy, dx
        cv.line([(34 * dx, 34 * dy), (190 * dx, 190 * dy)], 3.2)
        tip = 190
        for back, wide in ((tip - 34, 17), (tip - 62, 11)):
            b = (back * dx, back * dy)
            cv.line([(tip * dx, tip * dy), (b[0] + px * wide, b[1] + py * wide)], 2.4)
            cv.line([(tip * dx, tip * dy), (b[0] - px * wide, b[1] - py * wide)], 2.4)
    # rosette of leaves between the arrows
    for k in range(8):
        am = (k + 0.5) * math.pi / 4
        for sgn in (-1, 1):
            pts = []
            for t in range(0, 41):
                u = t / 40
                r = 62 + 112 * u
                a = am + sgn * 0.20 * math.sin(math.pi * u) ** 0.9 * (1.0 - 0.15 * u)
                pts.append((r * math.cos(a), r * math.sin(a)))
            cv.line(pts, 1.8)
    cv.circle(95, 2.2)
    for k in range(8):
        a = (k + 0.5) * math.pi / 4
        cv.circle(8, 2.0, 95 * math.cos(a), 95 * math.sin(a))
    cv.circle(34, 3.2)
    cv.circle(20, 1.6)
    cv.disc(9)
    return glowify(cv.array())


def sigil_arcane():
    cv = Canvas()
    rings_and_ticks(cv, 240, 30, 6)
    cv.circle(208, 2.8)
    for k in range(12):
        a = math.radians(k * 30)
        cx, cy = 222 * math.cos(a), 222 * math.sin(a)
        ux, uy = math.cos(a), math.sin(a)
        d = 7
        cv.line([(cx + ux * d, cy + uy * d), (cx - uy * d * .7, cy + ux * d * .7), (cx - ux * d, cy - uy * d), (cx + uy * d * .7, cy - ux * d * .7), (cx + ux * d, cy + uy * d)], 1.8)
        a2 = math.radians(k * 30 + 15)
        cv.disc(2.6, 222 * math.cos(a2), 222 * math.sin(a2))
    dashed_ring(cv, 180, 72, 0.6, 1.8)
    a0 = -math.pi / 2
    for off in (0, math.pi / 3):
        v = [(204 * math.cos(a0 + off + TAU * k / 3), 204 * math.sin(a0 + off + TAU * k / 3)) for k in range(4)]
        cv.line(v, 3.4)
    cv.circle(204, 1.6)
    cv.circle(118, 2.4)
    for k in range(6):
        a = a0 + math.pi / 6 + k * math.pi / 3
        cv.circle(10, 2.2, 118 * math.cos(a), 118 * math.sin(a))
        cv.line([(118 * math.cos(a), 118 * math.sin(a)), (62 * math.cos(a), 62 * math.sin(a))], 1.6)
    cv.circle(62, 2.6)
    for off in (0, math.pi / 3):
        v = [(60 * math.cos(a0 + off + TAU * k / 3), 60 * math.sin(a0 + off + TAU * k / 3)) for k in range(4)]
        cv.line(v, 2.2)
    cv.circle(24, 1.8)
    cv.disc(8)
    # tips of the big hexagram
    for k in range(6):
        a = a0 + k * math.pi / 3
        cv.circle(12, 2.2, 204 * math.cos(a), 204 * math.sin(a))
    return glowify(cv.array())


def sigil_hex():
    cv = Canvas()
    rings_and_ticks(cv, 240, 18, 6)
    cv.circle(210, 2.8)
    cv.circle(204, 1.4)
    a0 = -math.pi / 2
    star = [(204 * math.cos(a0 + TAU * (2 * k % 5) / 5), 204 * math.sin(a0 + TAU * (2 * k % 5) / 5)) for k in range(6)]
    cv.line(star, 3.6)
    # crescents outside the star tips
    for k in range(5):
        a = a0 + TAU * k / 5
        cx, cy = 222 * math.cos(a), 222 * math.sin(a)
        cv.disc(8.5, cx, cy)
        cv.disc(7.2, cx + math.cos(a + math.pi) * 3.5 * -1, cy + math.sin(a + math.pi) * 3.5 * -1, 0)
    for k in range(5):
        a = a0 + TAU * k / 5 + math.pi / 5
        cv.disc(2.6, 222 * math.cos(a), 222 * math.sin(a))
    # inner pentagon + sigil dots
    inner = [(78 * math.cos(a0 + math.pi + TAU * k / 5), 78 * math.sin(a0 + math.pi + TAU * k / 5)) for k in range(6)]
    cv.line(inner, 2.4)
    cv.circle(78, 1.8)
    for p in inner[:-1]:
        cv.circle(9, 2.0, *p)
    # the Darkhold eye
    top, bot = [], []
    for i in range(0, 61):
        x = -62 + 124 * i / 60
        h = 36 * (1 - (x / 62) ** 2) ** 0.85
        top.append((x, -h))
        bot.append((x, h * 0.9))
    cv.line(top, 3.0)
    cv.line(bot, 3.0)
    cv.circle(21, 3.0)
    cv.disc(10)
    for k in range(-3, 4):
        a = -math.pi / 2 + k * 0.17
        cv.line([(40 * math.cos(a), 40 * math.sin(a) - 14), (54 * math.cos(a), 54 * math.sin(a) - 16)], 1.6)
    dashed_ring(cv, 188, 60, 0.4, 1.6)
    return glowify(cv.array())


# ------------------------------------------------------------------ runes
def glyph(rng, h=40.0):
    pts = [(x, y) for x in (-1, 0, 1) for y in (-2, -1, 0, 1, 2)]
    strokes = []
    if rng.random() < 0.85:
        xs = rng.choice((-1, 0, 0, 1))
        strokes.append(((xs, -2), (xs, 2)))
    for _ in range(rng.randint(2, 4)):
        a, b = rng.sample(pts, 2)
        if a != b:
            strokes.append((a, b))
    s = h / 4.0
    return [((a[0] * s * 0.9, a[1] * s), (b[0] * s * 0.9, b[1] * s)) for a, b in strokes]


def rune_ring():
    cv = Canvas()
    rng = random.Random(7)
    cv.circle(240, 2.8)
    cv.circle(236, 1.0)
    cv.circle(186, 2.2)
    cv.circle(182, 1.0)
    n = 40
    for i in range(n):
        a = TAU * i / n
        cx, cy = 211 * math.cos(a), 211 * math.sin(a)
        ca, sa = math.cos(a + math.pi / 2), math.sin(a + math.pi / 2)  # glyph 'up' points outward
        for (x0, y0), (x1, y1) in glyph(rng, 38):
            def tr(x, y):
                return (cx + x * ca - y * -sa * -1 * -1 if False else cx + x * ca + y * (-sa) * -1 * -1, 0)
            # rotate (x, y) so +y -> outward direction
            ox, oy = math.cos(a), math.sin(a)
            tx, ty = -oy, ox
            P0 = (cx + x0 * tx - y0 * ox, cy + x0 * ty - y0 * oy)
            P1 = (cx + x1 * tx - y1 * ox, cy + x1 * ty - y1 * oy)
            cv.line([P0, P1], 2.3)
        a2 = a + TAU / n / 2
        cv.disc(2.2, 211 * math.cos(a2), 211 * math.sin(a2))
    return glowify(cv.array(), ((2.0, 0.8), (6.0, 0.5), (16.0, 0.22)))


# ------------------------------------------------------------------ hex honeycomb (tileable)
def hex_grid():
    s = 128.0
    W = int(3 * s)
    H = int(round(math.sqrt(3) * s))
    ys = (math.sqrt(3) * s) / H
    yy, xx = np.mgrid[0:H, 0:W].astype(float)
    xx += 0.5
    yy = (yy + 0.5) * ys
    best_d = np.full((H, W), 1e9)
    cx = np.zeros((H, W))
    cy = np.zeros((H, W))
    cid = np.zeros((H, W))
    for gi, (ox, oy) in enumerate(((0.0, 0.0), (1.5 * s, 0.5 * math.sqrt(3) * s))):
        for di in (-1, 0, 1):
            for dj in (-1, 0, 1):
                i = np.round((xx - ox) / (3 * s)) + di
                j = np.round((yy - oy) / (math.sqrt(3) * s)) + dj
                px = ox + i * 3 * s
                py = oy + j * math.sqrt(3) * s
                d = (xx - px) ** 2 + (yy - py) ** 2
                m = d < best_d
                best_d = np.where(m, d, best_d)
                cx = np.where(m, px, cx)
                cy = np.where(m, py, cy)
                cid = np.where(m, gi, cid)
    px, py = xx - cx, yy - cy
    a = math.sqrt(3) / 2 * s
    m = np.zeros_like(px)
    for ang in (math.radians(30), math.radians(90), math.radians(150)):
        m = np.maximum(m, np.abs(px * math.cos(ang) + py * math.sin(ang)))
    e = np.maximum(0, a - m)
    dv = np.full_like(px, 1e9)
    for k in range(6):
        vx, vy = s * math.cos(math.radians(60 * k)), s * math.sin(math.radians(60 * k))
        dv = np.minimum(dv, np.hypot(px - vx, py - vy))
    line = np.exp(-(e / 1.9) ** 2) + 0.42 * np.exp(-(e / 7.5) ** 2) + 0.18 * np.exp(-(e / 20) ** 2)
    verts = 0.9 * np.exp(-(dv / 10.0) ** 2)
    fill = np.where(cid == 0, 0.085, 0.035) + 0.05 * (1 - np.clip(m / a, 0, 1)) ** 2
    return line + verts + fill


# ------------------------------------------------------------------ soft sprites
def glow():
    n = 256
    y, x = np.mgrid[0:n, 0:n].astype(float)
    r = np.hypot(x - n / 2 + .5, y - n / 2 + .5) / (n / 2)
    i = 0.85 * np.exp(-(r / 0.13) ** 2) + 0.5 * np.exp(-(r / 0.34) ** 2) + 0.22 * np.exp(-(r / 0.7) ** 2)
    win = 1 - np.clip((r - 0.72) / 0.28, 0, 1) ** 1.5
    return i * win


def flare():
    n = 256
    y, x = np.mgrid[0:n, 0:n].astype(float)
    x = (x - n / 2 + .5) / (n / 2)
    y = (y - n / 2 + .5) / (n / 2)
    r = np.hypot(x, y)
    g = np.exp(-(r / 0.10) ** 2) * 1.0 + 0.35 * np.exp(-(r / 0.28) ** 2)
    sp = np.exp(-np.abs(x) * 3.2) * np.exp(-(y / 0.022) ** 2) + np.exp(-np.abs(y) * 3.2) * np.exp(-(x / 0.022) ** 2)
    u, v = (x + y) / math.sqrt(2), (x - y) / math.sqrt(2)
    sp2 = 0.55 * (np.exp(-np.abs(u) * 6) * np.exp(-(v / 0.018) ** 2) + np.exp(-np.abs(v) * 6) * np.exp(-(u / 0.018) ** 2))
    ring = 0.25 * np.exp(-((r - 0.55) / 0.015) ** 2)
    win = 1 - np.clip((r - 0.8) / 0.2, 0, 1)
    return (g + sp * 0.95 + sp2 + ring) * win


def beam():
    W, H = 512, 128
    v, u = np.mgrid[0:H, 0:W].astype(float)
    d = np.abs((v + .5) / H - 0.5) * 2
    u = u / W
    core = np.exp(-(d / 0.10) ** 2)
    halo = 0.5 * np.exp(-(d / 0.40) ** 2)
    mod = 0.82 + 0.18 * np.cos(TAU * u * 4)
    beads = 0.55 * np.exp(-((((u * 4) % 1) - 0.5) / 0.11) ** 2) * np.exp(-(d / 0.32) ** 2)
    streak = 0.18 * np.exp(-((d - 0.45) / 0.05) ** 2) * (0.5 + 0.5 * np.cos(TAU * u * 8 + 1.3))
    win = 1 - np.clip((d - 0.85) / 0.15, 0, 1)
    return (core + halo * mod + beads + streak) * win


def pillar():
    W, H = 256, 512
    rng = np.random.default_rng(3)
    y, x = np.mgrid[0:H, 0:W].astype(float)
    a = np.zeros((H, W))
    for _ in range(80):
        xc = rng.uniform(0, W)
        wd = rng.uniform(1.4, 5.5)
        ln = rng.uniform(140, 440)
        y0 = rng.uniform(0, H)
        br = rng.uniform(0.25, 1.0)
        dx = np.abs(x - xc)
        dx = np.minimum(dx, W - dx)
        t = ((y - y0) % H) / ln
        prof = np.where(t < 1, np.sin(np.pi * np.clip(t, 0, 1)) ** 1.5, 0)
        a += br * np.exp(-(dx / wd) ** 2) * prof
    bands = 0.10 * (0.5 + 0.5 * np.cos(TAU * y / H * 6))
    return np.clip(a, 0, 1.2) * 0.9 + bands * 0.6 + 0.06


def swirl():
    n = 512
    rng = np.random.default_rng(5)
    y, x = np.mgrid[0:n, 0:n].astype(float)
    x = (x - n / 2 + .5) / (n / 2)
    y = (y - n / 2 + .5) / (n / 2)
    r = np.hypot(x, y)
    th = np.arctan2(y, x)
    noise = gaussian_filter(rng.random((n, n)), 6)
    noise = (noise - noise.min()) / (noise.max() - noise.min())
    arms = 4
    ph = arms * th - 9.0 * r + 2.0 * (noise - .5)
    arm = (0.5 + 0.5 * np.cos(ph)) ** 3.0
    rad = np.clip((r - 0.03) / 0.12, 0, 1) * (1 - np.clip((r - 0.62) / 0.38, 0, 1)) * (0.45 + 0.55 * (1 - r))
    core = np.exp(-(r / 0.09) ** 2)
    thin = 0.35 * (0.5 + 0.5 * np.cos(arms * 2 * th - 14 * r)) ** 8 * rad
    return arm * rad * 0.95 + core * 0.9 + thin


def shock_ring():
    n = 512
    rng = np.random.default_rng(11)
    y, x = np.mgrid[0:n, 0:n].astype(float)
    x = (x - n / 2 + .5) / (n / 2)
    y = (y - n / 2 + .5) / (n / 2)
    r = np.hypot(x, y)
    th = np.arctan2(y, x)
    k = np.cos(th * 9) * 0.5 + np.cos(th * 23 + 1.0) * 0.5
    edge = np.exp(-((r - 0.92) / 0.018) ** 2) + 0.4 * np.exp(-((r - 0.92) / 0.05) ** 2)
    trail = np.where(r < 0.92, np.exp(-(0.92 - r) / 0.11) * (0.55 + 0.25 * k), 0) * 0.95
    fine = np.where(r < 0.92, 0.25 * np.exp(-((0.86 - r) / 0.012) ** 2), 0)
    win = 1 - np.clip((r - 0.94) / 0.06, 0, 1)
    return (edge + trail + fine) * win


# ------------------------------------------------------------------ ravens (normal alpha blend, 4 flap frames)
def raven_sheet():
    F, ss, FR = 128, 4, 4
    sheet = Image.new('RGBA', (F * FR, F), (0, 0, 0, 0))
    tip_y = [16, 42, 68, 98]
    bulges = [-16, -9, 0, 12]
    for fi in range(FR):
        ty = tip_y[fi]
        mask = Image.new('L', (F * ss, F * ss), 0)
        d = ImageDraw.Draw(mask)
        P = lambda p: (p[0] * ss, p[1] * ss)

        d.ellipse([P((56, 44)), P((72, 94))], fill=255)                      # body
        d.ellipse([P((57, 30)), P((71, 47))], fill=255)                      # head
        d.polygon([P((60.5, 33)), P((67.5, 33)), P((64, 17))], fill=255)     # beak
        d.polygon([P((56, 86)), P((72, 86)), P((79, 110)), P((71, 105)), P((64, 114)), P((57, 105)), P((49, 110))], fill=255)  # tail

        def wing(side):
            S = (64 + side * 3, 58)
            R = (64 + side * 4, 86)
            T = (64 + side * 56, ty)
            C = ((S[0] + T[0]) / 2, (S[1] + T[1]) / 2 + bulges[fi])
            pts = []
            for i in range(0, 25):
                t = i / 24
                x = (1 - t) ** 2 * S[0] + 2 * (1 - t) * t * C[0] + t ** 2 * T[0]
                y = (1 - t) ** 2 * S[1] + 2 * (1 - t) * t * C[1] + t ** 2 * T[1]
                pts.append((x, y))
            ex, ey = R[0] - T[0], R[1] - T[1]
            ln = math.hypot(ex, ey)
            nx, ny = (ey / ln, -ex / ln)
            if ny < 0:
                nx, ny = -nx, -ny      # push feathers away from the body, downwards
            for k in range(0, 7):
                t = k / 6
                bx, by = T[0] + ex * t, T[1] + ey * t
                reach = 10 * (1 - 0.35 * t) + 1
                pts.append((bx + nx * reach * 0.7 + side * reach * 0.5, by + ny * reach))
                if k < 6:
                    t2 = (k + 0.5) / 6
                    pts.append((T[0] + ex * t2 - nx * 1.5, T[1] + ey * t2 - ny * 1.5))
            pts.append(R)
            d.polygon([P(q) for q in pts], fill=255)

        wing(-1)
        wing(1)
        m = np.asarray(mask.resize((F, F), Image.LANCZOS), float) / 255.0
        inner = gaussian_filter(m, 1.4)
        rim = np.clip(m - inner * 0.92, 0, 1)
        rgb = np.zeros((F, F, 3)) + np.array([22, 12, 34])
        rgb += rim[..., None] * np.array([170, 95, 255]) * 1.4
        yy, xx = np.mgrid[0:F, 0:F]
        for ex_ in (61.0, 67.0):
            e = np.exp(-(((xx - ex_) ** 2 + (yy - 38.0) ** 2) / 2.0))
            rgb = rgb * (1 - e[..., None]) + e[..., None] * np.array([255, 120, 200])
        rgba = np.dstack([np.clip(rgb, 0, 255), m * 255]).astype(np.uint8)
        sheet.paste(Image.fromarray(rgba, 'RGBA'), (fi * F, 0))
    sheet.save(os.path.join(OUT, 'raven.png'), optimize=True)
    print('wrote raven', sheet.size)


if __name__ == '__main__':
    save('sigil_chaos', sigil_chaos())
    save('sigil_arcane', sigil_arcane())
    save('sigil_hex', sigil_hex())
    save('rune_ring', rune_ring())
    save('hex_grid', hex_grid())
    save('glow', glow())
    save('flare', flare())
    save('beam', beam())
    save('pillar', pillar())
    save('swirl', swirl())
    save('shock_ring', shock_ring())
    raven_sheet()
