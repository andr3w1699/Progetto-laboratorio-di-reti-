//package progetto_laboratorio_di_reti;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Classe che rappresenta la classifica di Wordle
 * 
 * @author Andrea Lepori
 *
 */
public class Classifica {

	// Struttura dati che rappresenta la classifica
	List<Utente> classifica;

	// metodo costruttore
	public Classifica() {
		// creo una LinkedList di utenti
		classifica = new LinkedList<Utente>();	
	}

	// METODI GETTER e SETTER
	// Dato che la classifica è una risorsa condivisa i metodi seguenti sono
	// sincronizzati attraverso l'uso del modificatore synchronized

	// metodo che aggiunge un utente alla classifica
	// restituisce true se e solo se l'utente è stato aggiunto alla classifica con
	// successo
	public synchronized boolean add(Utente u) throws NullPointerException {
		if (u == null)
			throw new NullPointerException();

		// se u non è già presente in classifica
		if (!classifica.contains(u)) {
			classifica.add(u); // lo aggiungo
			Collections.sort(classifica); // ordino per sicurezza
			return true;
		}

		return false;
	}

	/*
	 * metodo da invocare per tenere ordinata la classifica
	 * 
	 * @return true se e solo se è cambiato il podio della classifica
	 */
	public synchronized boolean sort() {
		// prendo il podio prima di ordinare
		List<String> old_podium = this.getPodium();
		// ordino
		Collections.sort(classifica);
		// prendo il podio dopo aver ordinato
		List<String> new_podium = this.getPodium();

		// Compares the specified object with this list for equality. Returns true if
		// and only if the specified object is also a list, bothlists have the same
		// size, and all corresponding pairs of elements inthe two lists are equal. (Two
		// elements e1 and e2 are equal if Objects.equals(e1, e2).)In other words, two
		// lists are defined to beequal if they contain the same elements in the same
		// order. Thisdefinition ensures that the equals method works properly
		// acrossdifferent implementations of the List interface.
		return !old_podium.equals(new_podium);
	}

	// restituisce la lista di stringhe di username degli utenti del podio della
	// classifica, se la classifica contiene 1 utente o 2 utenti il podio sarà
	// composto rispettivamente da 1 o 2 stringhe di username, se non ci sono utenti
	// in classifica restituisce null
	public synchronized List<String> getPodium() {
		List<String> podium = new ArrayList<>();
		if (classifica.size() >= 3) {
			podium.add(0, this.classifica.get(0).getUsername());
			podium.add(1, this.classifica.get(1).getUsername());
			podium.add(2, this.classifica.get(2).getUsername());
			return podium;
		} else if (classifica.size() == 2) {
			podium.add(0, this.classifica.get(0).getUsername());
			podium.add(1, this.classifica.get(1).getUsername());
			return podium;
		} else if (classifica.size() == 1) {
			podium.add(0, this.classifica.get(0).getUsername());
			return podium;

		}
		return podium;

	}

//@override 
	public synchronized String toString() {

		String result = "";
		int i = 1;
		for (Utente u : classifica) {
			if (!result.equals(""))
				result += "\n";
			result += i + "°: " + u.toString();
			i++;
		}
		return result;
	}

	// gets this.classifica
	public synchronized List<Utente> getClassifica() {
		return this.classifica;
	}
}
