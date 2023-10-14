//package progetto_laboratorio_di_reti;

import java.rmi.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.IOException;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// La classe RegistrationServiceImpl rappresenta l'implementazione del servizio che gestisce il database delle registrazioni.
// RegistrationServiceImpl implementa l'interfaccia RegistrationService quindi ne definisce i metodi.

public class RegistrationServiceImpl implements RegistrationService {

	/* Store data in a hashtable */
	// userò una hashtable di tuple <String,Utente> dove String è
	// l'username dell' Utente associato nella tupla
	// la stringa username essendo univoca fungerà da chiave
	// utilizzo una ConcurrentHashMap che essendo una struttura sincronizzata mi
	// esonera dalla sincronizzazione esplicita
	private ConcurrentHashMap<String, Utente> RegistrationDB;

	// classifica
	private Classifica c;

	/* Constructor - set up database */
	public RegistrationServiceImpl(Classifica classifica) throws RemoteException {
		// inizializzo la struttura chiamando il costruttore
		RegistrationDB = new ConcurrentHashMap<String, Utente>();
		// la classifica è quella passata al costruttore
		this.c = classifica;
		// se esiste il file con le registrazioni effettuate in passato le carico sulla
		// struttura del mio programma che implementa il database
		File stato = new File(System.getProperty("user.dir"), "LogRegistrazioni.txt");
		if (stato.exists()) {
			Reader r = null;
			try {
				r = Files.newBufferedReader(Paths.get(System.getProperty("user.dir"), "LogRegistrazioni.txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// deserializzazione
			Type mapOfMyClassObject = new TypeToken<ConcurrentHashMap<String, Utente>>() {
			}.getType();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			RegistrationDB = gson.fromJson(r, mapOfMyClassObject);
			for (Map.Entry<String, Utente> entry : this.RegistrationDB.entrySet()) { // scorro con un for generalizzato
																						// tutte le
				// entry della map
				c.add(entry.getValue());
			}
		}

	}

	// dato che la struttura RegistrationServiceImpl è una struttura condivisa tra
	// più threads i metodi getter e setter della classe sono protetti dal
	// modificatore synchronized da possibili corse critiche dovute all'accesso
	// concorrente

	// metodo per aggiungere la registrazione dell' utente, avente come credenziali
	// la coppia (username, password) al db delle registrazioni, restituisce un
	// booleano che mi dice se la registrazione è andata a buon fine.
	public synchronized boolean addRegistration(String username, String password) {
		// controllo che la password non sia la stringa vuota
		if (password.equals("")) {
			System.err.println("ERRORE: la password inserita (stringa vuota) non è valida");
			return false;
		}
		// controllo se è già presente un utente con quell'username nel db
		// serve a garantire l'univocità dell' username
		if (RegistrationDB.containsKey(username)) {
			System.err.println("ERRORE: è gia presente un utente con questo username");
			return false;
		}
		// arrivato a questo punto l'username immesso è univoco e la password non é
		// la stringa vuota
		// faccio una put nella hashtable, key --> username, value --> oggetto Utente al
		// cui costruttore passo username e password
		RegistrationDB.put(username, new Utente(username, password));
		// prendo l'utente appena creato dal database
		Utente u = RegistrationDB.get(username);
		// e lo metto in classifica
		c.add(u);
		System.out.printf("Utente: %s registrato correttamente con password:%s\n",
				RegistrationDB.get(username).getUsername(), RegistrationDB.get(username).getPassword());
		return true;
	}

	// metodo per registrare l'utente definito da username e password come logged
	// (effettua il login)
	// restituisce true se e solo se l' operazione è andata a buon fine
	public synchronized boolean setLogged(String username, String password) {

		// controllo che effettivamente sia registrato un utente con quell'username
		if (!this.isRegistered(username))
			return false;

		// reperisco l'utente registrato con quell'username
		Utente u = this.getUser(username);

		// controllo che l'utente non sia già loggato
		if (u.isLogged())
			return false;

		// controllo la validità delle credenziali con cui voglio fare login
		if (!u.getPassword().equals(password))
			return false;

		// reperisco l'oggetto utente associato ad username con una get e chiamo il
		// metodo log
		RegistrationDB.get(username).log();

		// stampe di controllo
		if (RegistrationDB.get(username).isLogged()) {// entro nel corpo dell'if solo se il flag logged è stato settato
														// a true
			System.out.printf("Utente: %s logged\n", RegistrationDB.get(username).getUsername());
			return true;
		}
		return false;

	}

	// metodo per registare l'utente definito da username e password passati in
	// input come unlogged
	// (realizza il logout) restituisce true se e solo se l'operazione è andata a
	// buon fine
	public synchronized boolean setUnlogged(String username, String password) {
		// controllo che effettivamente sia presente un utente con quell'username
		if (!this.isRegistered(username))
			return false;

		// reperisco l'utente registrato con quell'username
		Utente u = this.getUser(username);

		// controllo la validità delle credenziali con cui voglio fare login
		if (!u.getPassword().equals(password))
			return false;

		// controllo che l'utente non sia già unlogged
		if (!u.isLogged())
			return false;

		// reperisco l'oggetto utente associato ad username con una get e chiamo il
		// metodo unlog
		RegistrationDB.get(username).unlog();
		if (!RegistrationDB.get(username).isLogged()) {// entro nel corpo dell'if solo se il flag logged è stato settato
														// a false
			System.out.printf("Utente: %s unlogged\n", RegistrationDB.get(username).getUsername());
			return true;
		}
		return false;

	}

	// metodo per reperire l'oggetto utente associato alla stringa username
	// se questo è presente nel db delle registrazioni , null altrimenti
	public synchronized Utente getUser(String username) {
		// se è registrato un utente con username
		if (this.isRegistered(username))
			// uso il metodo get della hashtable
			return RegistrationDB.get(username);
		else {
			System.err.println("ERRORE: utente " + username+ " non presente nel database");
			return null;
		}
	}

	// metodo per reperire la password associata all' oggetto utente associato alla
	// stringa username se questo esiste nel db delle registrazione, null altrimenti
	public synchronized String getPassword(String username) {

		Utente u = this.getUser(username);
		if (u != null)
			return u.getPassword();

		else {
			System.err.println("ERRORE: utente " + username + " non presente nel database");
			return null;
		}
	}

	// metodo per verificare se l'utente con username passato in input è registrato
	// viene ritornato true se e solo se è registrato un utente con l'username
	// fornito in input nel db delle registrazioni
	// viene ritornato false se e solo se non è registrato nessun utente con
	// l'username fornito in input
	public synchronized boolean isRegistered(String username) {
		return RegistrationDB.containsKey(username);
	}

	// gets this classifica
	public synchronized Classifica getClassifica() throws RemoteException {
		return this.c;
	}

	// gets this registartionDB
	public synchronized ConcurrentHashMap<String, Utente> getRegistrazioni() throws RemoteException {
		return this.RegistrationDB;
	}

}