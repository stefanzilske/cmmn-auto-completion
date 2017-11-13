package de.holisticon.cmmn.autocompletion.sentry;

import org.camunda.bpm.engine.runtime.CaseExecution;
import org.springframework.stereotype.Component;

@Component
public class EverythingDoneSentry {

    public static final String NAME = "everythingDoneSentry";

    public Boolean isEverythingDone(CaseExecution execution) {
        return true;
    }
}
