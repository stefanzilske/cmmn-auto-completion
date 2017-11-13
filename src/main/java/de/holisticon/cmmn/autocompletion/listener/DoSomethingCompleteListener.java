package de.holisticon.cmmn.autocompletion.listener;

import org.camunda.bpm.engine.delegate.CaseExecutionListener;
import org.camunda.bpm.engine.delegate.DelegateCaseExecution;

public class DoSomethingCompleteListener implements CaseExecutionListener {

    public static final String NAME = "doSomethingCompleteListener";

    @Override
    public void notify(DelegateCaseExecution delegateCaseExecution) throws Exception {
    }
}
