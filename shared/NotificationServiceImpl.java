//package progetto_laboratorio_di_reti;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.List;

/**
 * Classe che implementa il servizio di notifica attraverso callback RMI. Dato
 * che questa classe implementa l'interfaccia NotificationService ne definisce i
 * metodi in essa dichiarati.
 * 
 * @author Andrea Lepori
 *
 */

@SuppressWarnings("serial")
public class NotificationServiceImpl extends RemoteObject implements NotificationService {
	/* lista dei client registrati */
	// realizzata come lista degli stub della classe NotifyEventInterface
	private List<NotifyEventInterface> clients;

	/* crea un nuovo servente */
	public NotificationServiceImpl() throws RemoteException {
		super();
		clients = new ArrayList<NotifyEventInterface>();
	};

	// i metodi di questa classe sono definiti synchronized
	// poichè la classe rappresenta una risorsa condivisa tra più threads

	/* registrazione per la callback */
	public synchronized void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
		if (!clients.contains(ClientInterface)) {
			clients.add(ClientInterface);
			System.out.println("New client registered.");
		}
	}

	/* annulla registrazione per il callback */
	public synchronized void unregisterForCallback(NotifyEventInterface Client) throws RemoteException {
		if (clients.remove(Client)) {
			System.out.println("Client unregistered");
		} else {
			System.out.println("Unable to unregister client.");
		}
	}

	/*
	 * notifica una variazione nelle prime 3 posizioni della classifica, quando
	 * viene richiamato, fa le callback a tutti i client registrati
	 */
	public void update(List<String> podium) throws RemoteException {
		doCallbacks(podium);
	};

	/* esegue le callback agli iscritti al servizio di notifica */
	private synchronized void doCallbacks(List<String> podium) throws RemoteException {
		System.out.println("Starting callbacks.");
		// open an iterator sugli iscritti al servizio di notifica
		Iterator<NotifyEventInterface> i = clients.iterator();
		// scorro l'iteratore
		while (i.hasNext()) {
			// prendo lo stub dell' oggetto remoto
			NotifyEventInterface client = (NotifyEventInterface) i.next();
			// invoco il metodo remoto
			client.notifyEvent(podium);
		}
		System.out.println("Callbacks complete.");
	}
}