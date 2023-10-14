//package progetto_laboratorio_di_reti;

import java.util.List;
import java.util.ArrayList;

/**
 * Classe che rappresenta una partita di Wordle.
 * 
 * @author Andrea Lepori
 *
 */
public class Partita {
	// lista che memorizza i tentativi fatti dall' utente nella partita
	// per ogni tentativo viene memorizzata la stringa dei suggerimenti
	// corrispondente all'invio di una gw rispetto alla sw in essere
	private List<String> tentativi;
	// numero di tentativi effettuato nella partita
	private int attempts;
	// flag che mi indica se la partita è terminata
	private boolean end;
	// flag che mi indica se ho vinto o meno
	private boolean win;

	// metodo costruttore
	public Partita() {
		// tentativi è un ArrayList
		tentativi = new ArrayList<>();
		win = false; // quando viene creata la partita non è vinta
		end = false; // quando viene creata la partita non è terminata
		attempts = 0; // inizialmente 0 tentativi fatti
	}

// METODI GETTER 
// dato che la risorsa Partita potrebbe essere condivisa 
// tra più threads i metodi sono dichiarati synchronized

	// metodo che restituisce il numero di tentativi effettuati in this partita
	public synchronized int getNumTentativi() {
		return this.attempts;
	}

	// restituisce una stringa che contiene tutte le stringhe dei tentativi
	// effettuati in this partita
	public synchronized String getTentativi() {
		String result = "";
		int i = 1;
		for (String s1 : tentativi) {
			result += i + "/12: " + s1 + "\n";
			i++;
		}
		return result;
	}

	// metodo che restituisce il valore del flag end
	public synchronized boolean isEnd() {
		return this.end;
	}

	// metodo che restituisce il valore del flag win
	public synchronized boolean isWin() {
		return this.win;
	}

	// METODI SETTER
	// dato che la risorsa Partita potrebbe essere condivisa
	// tra più threads i metodi sono dichiarati synchronized

	// metodo che setta il valore del flag end al valore passato in input
	public synchronized void setEnd(boolean end) {
		this.end = end;
	}

	// metodo che setta il valore del flag win al valore passato in input
	public synchronized void setWin(boolean win) {
		this.win = win;
	}

	// metodo che aggiunge un tentativo (rappresentato dalla stringa dei
	// suggerimenti) alla lista dei tentativi
	// la String suggerimento deve essere la stringa dei suggerimenti ottenuta per
	// la gw inviata rispetto alla sw attuale
	public synchronized void addTentativo(String suggerimento) {
		// se ho già effettuato 12 tentativi non posso più aggiungere tentativi alla
		// partita
		if (this.attempts >= 12) {
			return;
		}
		// aggiungo suggerimento alla lista coi suggerimenti
		this.tentativi.add(suggerimento);
		// incremento la variabile che conta i tentativi
		attempts++;
	}
}