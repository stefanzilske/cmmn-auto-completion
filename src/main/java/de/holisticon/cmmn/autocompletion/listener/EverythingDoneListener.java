package de.holisticon.cmmn.autocompletion.listener;

import org.camunda.bpm.engine.delegate.CaseExecutionListener;
import org.camunda.bpm.engine.delegate.DelegateCaseExecution;

public class EverythingDoneListener implements CaseExecutionListener {

    public static String NAME = "everythingDoneListener";

    @Override
    public void notify(DelegateCaseExecution delegateCaseExecution) throws Exception {
    }
}
