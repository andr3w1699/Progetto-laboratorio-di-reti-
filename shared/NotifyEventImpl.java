//package progetto_laboratorio_di_reti;

import java.rmi.*;
import java.rmi.server.*;
import java.util.List;
import java.util.ArrayList;

/**
 * 
 * Classe che implementa l’interfaccia NotifyEventInterface perciò fornisce la
 * definizione del metodo notifyEvent che può essere richiamato da remoto dal
 * servente per notificare il verificarsi di un determinato evento che in questo
 * è la variazione del podio.
 * 
 */

@SuppressWarnings("serial")
public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
	/* crea un nuovo callback client */
	// struttura che memorizza il podio della classifica 
	List<String> podium;

	// metodo costruttore 
	public NotifyEventImpl() throws RemoteException {
		super();
		podium = new ArrayList<>();
	}

	/*
	 * metodo che può essere richiamato dal servente per notificare un client di una
	 * variazione nel podio della classifica
	 */
	public void notifyEvent(List<String> podium) throws RemoteException {

		String returnMessage = "Update event received: variazione nel podio";
		System.out.println(returnMessage);

		// aggiorno il podio 
		this.podium = podium;
		// devo stampare il podio aggiornato 
		System.out.println(podium);

	}
}