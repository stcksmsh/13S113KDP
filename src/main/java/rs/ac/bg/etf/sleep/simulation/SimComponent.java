package rs.ac.bg.etf.sleep.simulation;

import java.util.*;

public interface SimComponent<V> {
	List<Event<V>> execute(Event<V> msg);

	List<Event<V>> init();

	String[] getState();

	void setState(String[] args);

	void restart(long time);

}