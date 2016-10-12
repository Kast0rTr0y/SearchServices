package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "workflow", "tasks", "sanity" })
public class GetTasksSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets all existing tasks with Rest API and response is successfull (200)")
    public void adminUserGetsAllTasks() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        tasksApi.getTasks().assertEntriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify asignee user gets its existing tasks with Rest API and response is successfull (200)")
    public void asigneeUserGetsItsTasks() throws Exception
    {
        restClient.authenticateUser(assigneeUser);
        tasksApi.getTasks().assertEntriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify candidate user gets its existing tasks with Rest API and response is successfull (200)")
    public void candidateUserGetsItsTasks() throws Exception
    {
        restClient.authenticateUser(assigneeUser);
        tasksApi.getTasks().assertEntriesListIsNotEmpty();
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
