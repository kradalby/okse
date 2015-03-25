package no.ntnu.okse.protocol.wsn;

import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.internal.ServiceConnection;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Created by Trond Walleraunet on 25.03.2015.
 */
public class WSNTransparentHUB implements Hub {

    private WSNotificationServer _server;

    public WSNTransparentHUB() {
        this._server = WSNotificationServer.getInstance();
    }

    @Override
    public InternalMessage acceptNetMessage(InternalMessage internalMessage, OutputStream outputStream) {
        return null;
    }

    @Override
    public InternalMessage acceptLocalMessage(InternalMessage internalMessage) {
        return null;
    }

    @Override
    public String getInetAdress() {
        return WSNotificationServer.getURI();
    }

    @Override
    public void registerService(ServiceConnection serviceConnection) {

    }

    @Override
    public void removeService(ServiceConnection serviceConnection) {

    }

    @Override
    public boolean isServiceRegistered(ServiceConnection serviceConnection) {
        return false;
    }

    @Override
    public Collection<ServiceConnection> getServices() {
        return null;
    }
}
