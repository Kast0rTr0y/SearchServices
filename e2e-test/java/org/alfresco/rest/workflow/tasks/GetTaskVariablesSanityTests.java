package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestVariableModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */

public class GetTaskVariablesSanityTests extends RestTest
{
    private UserModel userModel, userWhoStartsTask, assignee;
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;
    private RestVariableModelsCollection variableModels;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assignee);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that user that started the process gets task variables")
    public void getTaskVariablesByUserWhoStartedProcess() throws Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        variableModels = restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that user that is involved in the process gets task variables")
    public void getTaskVariablesByUserInvolvedInProcess() throws Exception
    {
        restClient.authenticateUser(assignee);
        variableModels = restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that user that is not involved in the process gets task variables")
    public void getTaskVariablesUsingAnyUser() throws Exception
    {
        UserModel randomUser = dataUser.createRandomTestUser();

        restClient.authenticateUser(randomUser);
        restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that admin is able to  task variables")
    public void getTaskVariablesUsingAdmin() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();

        restClient.authenticateUser(adminUser);
        variableModels = restClient.withWorkflowAPI().usingTask(taskModel).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModels.assertThat().entriesListIsNotEmpty();
    }
}