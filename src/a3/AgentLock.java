package a3;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AgentLock {
	
    public ReentrantLock agentLock;
    public Condition assignCondition;    
    
    public AgentLock() {    	
    	agentLock = new ReentrantLock();
    	assignCondition = agentLock.newCondition();    	
    }
}
