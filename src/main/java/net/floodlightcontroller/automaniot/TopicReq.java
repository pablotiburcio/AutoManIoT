package net.floodlightcontroller.automaniot;

public class TopicReq {
	private String topic;
	private int min,max,timeout;
	
	public TopicReq(String topic, int min, int max, int timeout){
		this.topic = topic;
		this.min = min;
		this.max = max;
		this.timeout = timeout;
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
				this.min + " " +
				this.max + " " +
				this.timeout;
	}
}
