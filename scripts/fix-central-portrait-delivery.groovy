import info.magnolia.commands.CommandsManager
import info.magnolia.context.MgnlContext

/*
 * Entfernt die fehlerhafte DAM-Referenz, die trotz .webp-Pfad ein großes PNG
 * ausliefert. Die Templates verwenden danach wieder die versionierten,
 * optimierten WebP-/JPEG-Dateien aus dem Light Module.
 */

def siteSettings = MgnlContext.getJCRSession('siteSettings')
siteSettings.refresh(false)

def settingsPath = '/cleonhardt/Logo-Cleonhardt'
if (!siteSettings.nodeExists(settingsPath)) {
    throw new IllegalStateException("Website-Einstellungen nicht gefunden: ${settingsPath}")
}

def settings = siteSettings.getNode(settingsPath)
if (settings.hasProperty('portraitImage')) {
    settings.getProperty('portraitImage').remove()
    siteSettings.save()
    println 'Fehlerhafte zentrale Porträt-Referenz entfernt; optimierter Template-Fallback ist aktiv.'
} else {
    println 'Keine zentrale Porträt-Referenz vorhanden; optimierter Template-Fallback ist bereits aktiv.'
}

def parameters = new LinkedHashMap<String, Object>()
parameters.put('repository', 'siteSettings')
parameters.put('path', settingsPath)
parameters.put('recursive', true)
def result = CommandsManager.getInstance().executeCommand('default', 'publish', parameters)
println "${settingsPath}: ${result ? 'veröffentlicht' : 'Veröffentlichung nicht bestätigt'}"
