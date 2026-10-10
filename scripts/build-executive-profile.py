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
        "Ich führe Produktteams durch Veränderung. Mein nächster Schritt: Head of Product.",
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
        "Product Leader mit Erfahrung in Handel, Omnichannel, CRM und MarTech. Baut interne Produktteams auf, entwickelt Product Owner und stabilisiert kritische Programme. Verbindet Kundennutzen, Produktstrategie und technische Entscheidungen. Sucht eine feste Head-of-Product-Rolle im Handels- oder Plattformumfeld. Bei ALDI Nord und P&amp;C: fachliche Führung, keine klassische Linienverantwortung.",
        body_style,
        left,
        y_left,
        left_width,
    )
    y_left -= 9 * mm

    label(c, "Ausgewählte Beiträge und Ergebnisse", left, y_left)
    y_left -= 6 * mm

    mandates = [
        (
            "ALDI Nord - Mobile, Loyalty &amp; CRM",
            "Externe Agenturstrukturen in interne Produktteams mit 14 und 12 Personen überführt. Rollen, Auswahl und Arbeitsweisen mit aufgebaut; Teams fachlich geführt. Strategie und Delivery in einem Operating Model verbunden. Ergebnis: interne Fähigkeiten, ein stabiler Release-Rhythmus und eine bessere Datenbasis durch Adjust.",
        ),
        (
            "P&amp;C / Fashion ID - Plattform-Relaunch &amp; New Retail",
            "Den kompletten Plattform-Relaunch im New-Retail-Kontext umgesetzt. Verantwortung von SEO, Seitenstruktur und Kampagnenseiten bis Produktdetailseite, MyAccount (Login, Registrierung), Newsletter-Anmeldung, Loyalty-Inhalten, Loyalty-Integration und Recommendations. Translation Management, Styleguide, Design System und Brand-Integration für P&amp;C, ANSON'S und Stylebop. A/B-Testing und UX; GA4-Integration, Optimierung mit MarTech und Exponea Web-Layer. Criteo-Beratung und Integration. Product Owner entwickelt, PO-Community und Operating Model mitgestaltet. Suche und Produktlistings: Schnittstelle zum Nachbarteam.",
        ),
        (
            "SUNZINET - Internationaler E-Commerce",
            "Ein verzögertes Relaunch-Programm mit unvollständigen Anforderungen übernommen. Roadmap, Prioritäten und Migration neu geordnet; ein Team mit 6 Personen fachlich geführt. Ergebnis: steuerbare Umsetzung, abgeschlossener internationaler Rollout und zurückgewonnenes Kundenvertrauen.",
        ),
        (
            "neusta / REWE Group - Relaunch und Weiterentwicklung",
            "Den Relaunch in einer kritischen Phase stabilisiert und abgeschlossen. Danach Zugänglichkeit, Navigation, Pressebereich und Performance weiterentwickelt, Cloud Hosting begleitet und Deployment-Prozesse deutlich beschleunigt.",
        ),
    ]

    for title, body in mandates:
        y_left = paragraph(c, title, item_title_style, left, y_left, left_width)
        y_left -= 1.5 * mm
        y_left = paragraph(c, body, small_style, left, y_left, left_width)
        y_left -= 5.2 * mm

    label(c, "Technische Urteilskraft", left, y_left)
    y_left -= 5 * mm
    y_left = paragraph(
        c,
        "ALDI: Native Apps, Backend for Frontend, Azure, Kubernetes, Redis, Salesforce, SAP CDP, GK Engage, Adjust.<br/>P&amp;C: Headless React, Node.js, Kotlin, Google Cloud, GA4, Exponea, OneTrust, Criteo.<br/>Delivery: Magnolia CMS, OpenCms, Shopware, TYPO3, ERP-Integration.",
        small_style,
        left,
        y_left,
        left_width,
    )

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
    label(c, "Stationen - aktuell zuerst", right_x, y_right)
    y_right -= 6 * mm
    stations = [
        ("ALDI Nord", "Senior Manager / Product Lead Mobile, Loyalty &amp; CRM"),
        ("P&amp;C / Fashion ID", "Senior / Lead Product Owner, Welcome Domain"),
        ("SUNZINET", "Senior Projektmanager / Product Owner"),
        ("neusta webservices", "Projektmanager / Product Owner"),
        ("Bundeswehr", "Abteilungsleiter Materialwirtschaft"),
        ("Firma Habuzin", "Kaufmann im Einzelhandel / Leitung TV-Abteilung"),
    ]
    for employer, role in stations:
        y_right = paragraph(c, f"<b>{employer}</b><br/>{role}", small_style, right_x, y_right, right_width)
        y_right -= 3 * mm

    y_right -= 9 * mm
    label(c, "Kontakt", right_x, y_right)
    y_right -= 6 * mm
    contact_lines = [
        ("cleonhardt.de", "https://cleonhardt.de/"),
        ("leonhardtchristian@icloud.com", "mailto:leonhardtchristian@icloud.com"),
        ("LinkedIn", "https://de.linkedin.com/in/christian-leonhardt-b341687b"),
    ]
    for text, url in contact_lines:
        c.setFillColor(INK)
        contact_font_size = min(8, 8 * right_width / pdfmetrics.stringWidth(text, "CLSans-Bold", 8))
        c.setFont("CLSans-Bold", contact_font_size)
        c.drawString(right_x, y_right, text)
        c.linkURL(url, (right_x, y_right - 2, right, y_right + 9), relative=0)
        y_right -= 5.2 * mm

    footer_y = 13 * mm
    assert min(y_left, y_right) > footer_y + 9 * mm, "Inhalt überlappt den Seitenfuß."
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
