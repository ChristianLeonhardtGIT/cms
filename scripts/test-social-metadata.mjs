import assert from 'node:assert/strict';

const baseUrl = process.env.BASE_URL || 'https://cleonhardt.de';
const pages = [
  ['/', 'WebPage'],
  ['/ueber-mich', 'ProfilePage'],
  ['/insights', 'CollectionPage'],
  ['/kontakt', 'ContactPage'],
  ['/insights/diagnose-vor-eingriff', 'WebPage']
];

const metaContent = (html, attribute, value) => {
  const escaped = value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const expression = new RegExp(`<meta[^>]+${attribute}="${escaped}"[^>]+content="([^"]+)"`, 'i');
  return html.match(expression)?.[1] || '';
};

for (const [path, expectedPageType] of pages) {
  const response = await fetch(`${baseUrl}${path}`);
  assert.equal(response.status, 200, `${path} is unavailable`);
  const html = await response.text();

  assert.equal(metaContent(html, 'name', 'twitter:card'), 'summary_large_image', `${path} lacks a large Twitter card`);
  assert.equal(metaContent(html, 'property', 'og:image:type'), 'image/png', `${path} lacks OG image type`);
  assert.equal(metaContent(html, 'property', 'og:image:width'), '1200', `${path} has wrong OG image width`);
  assert.equal(metaContent(html, 'property', 'og:image:height'), '630', `${path} has wrong OG image height`);
  const socialImage = metaContent(html, 'property', 'og:image');
  assert.ok(socialImage.startsWith(`${baseUrl}/`), `${path} has no absolute OG image`);
  assert.equal(metaContent(html, 'property', 'og:image:secure_url'), socialImage, `${path} has inconsistent secure OG image`);
  assert.ok(metaContent(html, 'property', 'og:image:alt'), `${path} lacks OG image alternative text`);
  assert.ok(metaContent(html, 'name', 'twitter:image:alt'), `${path} lacks Twitter image alternative text`);

  const jsonLdMatch = html.match(/<script type="application\/ld\+json">\s*([\s\S]*?)\s*<\/script>/i);
  assert.ok(jsonLdMatch, `${path} lacks JSON-LD`);
  const jsonLd = JSON.parse(jsonLdMatch[1]);
  const graph = jsonLd['@graph'];
  assert.ok(Array.isArray(graph), `${path} has no JSON-LD graph`);
  assert.ok(graph.some((item) => item['@type'] === expectedPageType), `${path} lacks ${expectedPageType} schema`);
  assert.ok(graph.some((item) => item['@type'] === 'ImageObject'), `${path} lacks ImageObject schema`);
  assert.ok(!graph.some((item) => ['Service', 'Offer', 'AggregateOffer'].includes(item['@type'])), `${path} exposes paused offer schema`);

  if (path.startsWith('/insights/')) {
    assert.equal(metaContent(html, 'property', 'og:type'), 'article', `${path} is not an OG article`);
    assert.ok(graph.some((item) => item['@type'] === 'Article'), `${path} lacks Article schema`);
    assert.ok(graph.some((item) => item['@type'] === 'BreadcrumbList'), `${path} lacks breadcrumb schema`);
  }
}

const imageResponse = await fetch(`${baseUrl}/.resources/meine-website/webresources/images/christian-leonhardt-social-card.png?v=20260805-1`);
assert.equal(imageResponse.status, 200, 'Social card image is unavailable');
assert.match(imageResponse.headers.get('content-type') || '', /image\/png/i, 'Social card is not delivered as PNG');

console.log('Social metadata passed.');
