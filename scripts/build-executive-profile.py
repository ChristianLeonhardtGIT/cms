from pathlib import Path

from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "christian-leonhardt-executive-profile.pdf"

INK = HexColor("#10231d")
GREEN = HexColor("#174e3b")
MUTED = HexColor("#5d6761")
PAPER = HexColor("#f3f0e8")
SIGNAL = HexColor("#d9ff63")
LINE = HexColor("#c9c7be")


def register_fonts():
    font_dir = Path("/usr/share/fonts/TTF")
    pdfmetrics.registerFont(TTFont("CLSans", str(font_dir / "DejaVuSans.ttf")))
    pdfmetrics.registerFont(TTFont("CLSans-Bold", str(font_dir / "DejaVuSans-Bold.ttf")))
    pdfmetrics.registerFont(TTFont("CLSerif-Italic", str(font_dir / "DejaVuSerif-Italic.ttf")))


def paragraph(c, text, style, x, y, width):
    item = Paragraph(text, style)
    _, block_height = item.wrap(width, 100 * mm)
    item.drawOn(c, x, y - block_height)
    return y - block_height


def label(c, text, x, y):
    c.setFont("CLSans-Bold", 7.2)
    c.setFillColor(GREEN)
    c.drawString(x, y, text.upper())


def build():
    register_fonts()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=A4)
    width, height = A4
    c.setTitle("Christian Leonhardt - Executive Profile")
    c.setAuthor("Christian Leonhardt")
    c.setSubject("Product Leadership, People Leadership and digital product organizations")

    c.setFillColor(PAPER)
    c.rect(0, 0, width, height, stroke=0, fill=1)
    c.setFillColor(GREEN)
    c.rect(0, height - 8 * mm, width, 8 * mm, stroke=0, fill=1)

    left = 18 * mm
    right = width - 18 * mm
    top = height - 22 * mm

    c.setFillColor(GREEN)
    c.circle(left + 6 * mm, top - 2 * mm, 6 * mm, stroke=0, fill=1)
    c.setFillColor(SIGNAL)
    c.setFont("CLSans-Bold", 9)
    c.drawCentredString(left + 6 * mm, top - 3.2 * mm, "CL")

    c.setFillColor(INK)
    c.setFont("CLSans-Bold", 22)
    c.drawString(left + 17 * mm, top + 1.5 * mm, "Christian Leonhardt")
    c.setFont("CLSans-Bold", 8.2)
    c.setFillColor(GREEN)
    c.drawString(left + 17 * mm, top - 5 * mm, "PRODUCT LEADERSHIP  ·  PEOPLE LEADERSHIP  ·  TECHNOLOGY")

    statement_style = ParagraphStyle(
        "statement",
        fontName="CLSerif-Italic",
        fontSize=15,
        leading=19,
        textColor=GREEN,
        spaceAfter=0,
        alignment=TA_LEFT,
    )
    body_style = ParagraphStyle(
        "body",
        fontName="CLSans",
        fontSize=8.6,
        leading=12.2,
        textColor=INK,
        spaceAfter=0,
    )
    small_style = ParagraphStyle(
        "small",
        fontName="CLSans",
        fontSize=7.8,
        leading=10.8,
        textColor=MUTED,
        spaceAfter=0,
    )
    item_title_style = ParagraphStyle(
        "item-title",
        fontName="CLSans-Bold",
        fontSize=9.2,
        leading=12,
        textColor=INK,
        spaceAfter=0,
    )

    y = top - 18 * mm
    y = paragraph(
        c,
        "Ich baue Produktorganisationen, die Strategie in wirksame digitale Produkte übersetzen.",
        statement_style,
        left,
        y,
        right - left,
    )
    y -= 5 * mm
    c.setStrokeColor(LINE)
    c.setLineWidth(0.6)
    c.line(left, y, right, y)
    y -= 9 * mm

    col_gap = 10 * mm
    left_width = 112 * mm
    right_x = left + left_width + col_gap
    right_width = right - right_x

    label(c, "Profil", left, y)
    y_left = y - 5 * mm
    y_left = paragraph(
        c,
        "Product and people leader mit Erfahrung im Aufbau und in der Transformation von Produktorganisationen in Retail, E-Commerce, digitalen Plattformen und operativen Umfeldern. Verbindet Produktstrategie, Organisationsdesign, Teamentwicklung und technische Entscheidungen. Führt über Klarheit, explizite Trade-offs und echte Verantwortung.",
        body_style,
        left,
        y_left,
        left_width,
    )
    y_left -= 9 * mm

    label(c, "Ausgewählte Leadership-Mandate", left, y_left)
    y_left -= 6 * mm

    mandates = [
        (
            "ALDI Nord - Mobile, Loyalty & CRM",
            "Strategie, Roadmaps, Business Cases, Technologieentscheidungen und Delivery in einem Operating Model verbunden. Externe Agenturstrukturen in interne Produktteams mit Teamgrößen von 14 und 12 Personen überführt. Technischer Kontext: Native Apps, Backend for Frontend, Azure, Kubernetes, Redis, Salesforce, SAP, GK Engage und Adjust.",
        ),
        (
            "Peek & Cloppenburg / Fashion ID - Welcome Domain",
            "Das digitale Eingangserlebnis über Web und App als zusammenhängenden Produktbereich geführt. Strategie, OKRs, Roadmaps, Discovery und Delivery mit klaren Verantwortungsräumen verbunden. Technischer Kontext: Headless React Frontend, Node.js und Kotlin Backend Services sowie Google Cloud.",
        ),
        (
            "SUNZINET - Internationaler E-Commerce",
            "Ein gefährdetes Relaunch-Programm neu ausgerichtet und Shop-Ersatz, ERP-Integration, Hosting und Rollout in eine belastbare Roadmap, klare Entscheidungen und eine schrittweise Migration überführt. Führung eines interdisziplinären Teams mit 6 Personen.",
        ),
    ]

    for title, body in mandates:
        y_left = paragraph(c, title, item_title_style, left, y_left, left_width)
        y_left -= 1.5 * mm
        y_left = paragraph(c, body, small_style, left, y_left, left_width)
        y_left -= 5.2 * mm

    y_right = y
    label(c, "Leadership-Fokus", right_x, y_right)
    y_right -= 6 * mm
    focus_items = [
        ("Product Strategy", "Vision, Priorisierung, Roadmaps und Entscheidungslogik"),
        ("People Leadership", "Rollen, Feedback, Coaching und Capability Building"),
        ("Operating Models", "Verantwortung, Governance und Discovery-to-Delivery"),
        ("Transformation", "Internalisierung, Programmstabilisierung und neue Arbeitsweisen"),
    ]
    for title, body in focus_items:
        c.setFillColor(SIGNAL)
        c.circle(right_x + 1.4 * mm, y_right - 1.1 * mm, 1.2 * mm, stroke=0, fill=1)
        y_right = paragraph(
            c,
            f"<b>{title}</b><br/>{body}",
            small_style,
            right_x + 5 * mm,
            y_right + 1.8 * mm,
            right_width - 5 * mm,
        )
        y_right -= 4 * mm

    y_right -= 3 * mm
    label(c, "Arbeitsprinzipien", right_x, y_right)
    y_right -= 6 * mm
    y_right = paragraph(
        c,
        "Kontext vor Standard.<br/>Mandate statt Mikromanagement.<br/>Trade-offs sichtbar machen.<br/>Wirkung beobachten und zurückführen.",
        body_style,
        right_x,
        y_right,
        right_width,
    )

    y_right -= 9 * mm
    label(c, "Kontakt", right_x, y_right)
    y_right -= 6 * mm
    contact_lines = [
        ("cleonhardt.de", "https://cleonhardt.de/"),
        ("kontakt@cleonhardt.de", "mailto:kontakt@cleonhardt.de"),
        ("LinkedIn", "https://de.linkedin.com/in/christian-leonhardt-b341687b"),
    ]
    for text, url in contact_lines:
        c.setFillColor(INK)
        c.setFont("CLSans-Bold", 8)
        c.drawString(right_x, y_right, text)
        c.linkURL(url, (right_x, y_right - 2, right, y_right + 9), relative=0)
        y_right -= 5.2 * mm

    footer_y = 13 * mm
    c.setStrokeColor(LINE)
    c.line(left, footer_y + 5 * mm, right, footer_y + 5 * mm)
    c.setFillColor(MUTED)
    c.setFont("CLSans", 6.8)
    c.drawString(left, footer_y, "Executive Profile - öffentliches Kurzprofil ohne vertrauliche Geschäftskennzahlen")
    c.drawRightString(right, footer_y, "Christian Leonhardt")

    c.showPage()
    c.save()


if __name__ == "__main__":
    build()
