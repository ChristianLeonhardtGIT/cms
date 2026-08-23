import info.magnolia.context.MgnlContext
import info.magnolia.keystore.PasswordManagerKeyPair
import info.magnolia.objectfactory.Components
import java.util.function.Supplier
import javax.jcr.Session

/*
 * Einmalig auf der Author-Instanz ausführen, wenn die private Schlüsseldatei
 * des Magnolia Password Managers noch nicht vorhanden ist.
 */

PasswordManagerKeyPair keyPair = Components.getComponent(PasswordManagerKeyPair)
Supplier<Session> configSession = {
    MgnlContext.getJCRSession('config')
} as Supplier<Session>

keyPair.initializeKeys(configSession)

println 'Magnolia Password Manager wurde initialisiert.'
