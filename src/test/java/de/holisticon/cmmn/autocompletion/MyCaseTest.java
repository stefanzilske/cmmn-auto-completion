package de.holisticon.cmmn.autocompletion;

import de.holisticon.cmmn.autocompletion.listener.DoSomethingCompleteListener;
import de.holisticon.cmmn.autocompletion.listener.EverythingDoneListener;
import de.holisticon.cmmn.autocompletion.sentry.EverythingDoneSentry;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.runtime.CaseInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.MockExpressionManager;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.mockito.DelegateExpressions;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;
import static org.camunda.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.cmmn.CmmnAwareTests.*;

/**
 * Created by zilskes on 22.05.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class MyCaseTest {

    private final ProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration(){{
        this.jobExecutorActivate = false;
        this.expressionManager = new MockExpressionManager();
        this.databaseSchemaUpdate = ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE;
        this.isMetricsEnabled=false;
    }};


    @Rule
    public ProcessEngineRule rule = new ProcessEngineRule(configuration.buildProcessEngine());

    @Mock
    private EverythingDoneSentry everythingDoneSentry;

    @Mock
    private DoSomethingCompleteListener doSomethingCompleteListener;

    @Mock
    private EverythingDoneListener everythingDoneListener;

    @Test
    @Deployment(resources = "my_case.cmmn")
    public void caseInstance_shouldEnd_afterMultipleKonditionenWereDeactivated() throws Exception {
        // register some mocks
        registerSubProcessMockWithWaitState();
        Mocks.register(DoSomethingCompleteListener.NAME, doSomethingCompleteListener);
        Mocks.register(EverythingDoneListener.NAME, everythingDoneListener);
        Mocks.register(EverythingDoneSentry.NAME, everythingDoneSentry);

        // initially, the sentry's if-part should evaluate to false
        Mockito.when(everythingDoneSentry.isEverythingDone(Mockito.any())).thenReturn(false);

        // start the case instance
        CaseInstance caseInstance = caseService().createCaseInstanceByKey("my_case", "4711");

        // Start the process task twice
        manuallyStart(caseExecution(caseExecutionQuery().activityId("process_task_do_something").enabled(), caseInstance));
        manuallyStart(caseExecution(caseExecutionQuery().activityId("process_task_do_something").enabled(), caseInstance));

        // there should be two running instances of the sub process, with two user tasks waiting
        assertThat(runtimeService().createProcessInstanceQuery().processDefinitionKey("do_something").list().size()).isEqualTo(2);
        List<Task> tasks = taskService().createTaskQuery().active().list();
        assertThat(tasks.size()).isEqualTo(2);

        // complete first user tasks and thereby end the first process
        taskService().complete(tasks.get(0).getId());

        // one process instance left
        assertThat(runtimeService().createProcessInstanceQuery().processDefinitionKey("do_something").list().size()).isEqualTo(1);

        // the case instance is still active
        assertThat(caseInstance).isActive();

        // now the sentry's if-part should evaluate to true
        Mockito.when(everythingDoneSentry.isEverythingDone(Mockito.any())).thenReturn(true);

        // complete second user tasks and thereby end the second process
        taskService().complete(tasks.get(1).getId());

        // The milestone should be completed by now
        assertThat(caseInstance).milestone("milestone_everything_done").isCompleted();

        // Now we expect that the case instance itself is completed
        assertThat(caseInstance).isCompleted();
    }

    private static void registerSubProcessMockWithWaitState() {
        BpmnModelInstance doSomething = Bpmn.createExecutableProcess("do_something").startEvent().userTask().endEvent().done();
        repositoryService().createDeployment().addModelInstance("mock/do_something.bpmn", doSomething).deploy();
    }
}
