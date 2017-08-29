package net.floodlightcontroller.automaniot;

public class TopicReq {
	private String topic;
	private int min,max,timeout, requisite;
	
	/*
	 * TODO: Put requisite a Map or Static attribute
	 * Requisite = 1 -> delay/latency
	 * Requisite = 2 -> bandwidth
	 * Requisite = 3 -> mobility
	 * Requisite = 4 -> priority
	 * Requisite = 5 -> security
	 * Requisite = 6 -> reliability
	 */
	
	public TopicReq(String topic, int requisite, int min, int max, int timeout){
		this.topic = topic;
		this.min = min;
		this.max = max;
		this.timeout = timeout;
		this.setRequisite(requisite);
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String toString(){
		return  this.topic + " " +
				this.requisite + " " +
				this.min + " " +
				this.max + " " +
				this.timeout;
	}

	public int getRequisite() {
		return requisite;
	}

	public void setRequisite(int requisite) {
		this.requisite = requisite;
	}
}
