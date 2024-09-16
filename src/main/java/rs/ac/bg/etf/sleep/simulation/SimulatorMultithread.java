package rs.ac.bg.etf.sleep.simulation;

public class SimulatorMultithread<T> extends Simulator<T> {

	public SimulatorMultithread(int id) {
		super(id);
	}

	private void synchronize() {
		// TODO Auto-generated method stub
	}

	private boolean isTimeInTheRange(Event<T> m) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void execute() {
		Event<T> m = queue.getEvent();
		if (!isTimeInTheRange(m)) {
			queue.putEvent(m);
			synchronize();
			m = queue.getEvent();
		}
		lastEvent = m;
		System.out.println("Simulator " + id + " executing event " + m.getDstID() + " " + m.getSrcID() + " " + m.getlTime());
		lTime = lastEvent.lTime;
		if (lastEvent.ok()) {
			work(lastEvent);
		}
	}
}
