import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const pageTemplate = await readFile('light-modules/meine-website/templates/pages/home.ftl', 'utf8');
const pageDefinition = await readFile('light-modules/meine-website/templates/pages/home.yaml', 'utf8');
const faqTemplate = await readFile('light-modules/meine-website/templates/components/faq.ftl', 'utf8');
const faqDialog = await readFile('light-modules/meine-website/dialogs/components/faq.yaml', 'utf8');
const css = await readFile('light-modules/meine-website/webresources/css/site.css', 'utf8');
const faqContent = await readFile('scripts/implement-public-faqs.groovy', 'utf8');

assert.match(pageTemplate, /christian-leonhardt-chos-signet-invoice\.png/);
assert.doesNotMatch(pageTemplate, /"logo"\s*:\s*\{[\s\S]{0,180}"url"\s*:\s*"\$\{socialImageUrl/);
assert.match(pageTemplate, /currentInsightIndex/);
assert.match(pageTemplate, /\(currentInsightIndex \+ offset\) % allInsights\?size/);

assert.match(pageDefinition, /faq:\s*\n\s+id: meine-website:components\/faq/);
assert.match(faqDialog, /\$type: jcrMultiField/);
assert.match(faqDialog, /question:/);
assert.match(faqDialog, /answer:/);
assert.match(faqTemplate, /<details class="faq-item">/);
assert.match(faqTemplate, /"@type": "FAQPage"/);
assert.match(faqTemplate, /"@type": "Question"/);
assert.match(faqTemplate, /"@type": "Answer"/);
assert.match(css, /\.faq-section/);
assert.match(css, /\.faq-item summary:focus-visible/);
assert.match(faqContent, /'\/home'/);
assert.match(faqContent, /'\/chos'/);
assert.match(faqContent, /'\/insights'/);
assert.doesNotMatch(faqContent, /'\/clarity-session'\s*:/);

console.log('SEO-Prio 1 und FAQ-Modul: Strukturtests erfolgreich.');
