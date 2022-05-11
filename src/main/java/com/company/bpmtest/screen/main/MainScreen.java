package com.company.bpmtest.screen.main;

import io.jmix.bpm.entity.ProcessDefinitionData;
import io.jmix.bpm.entity.UserGroup;
import io.jmix.bpm.service.UserGroupService;
import io.jmix.bpm.util.FlowableEntitiesConverter;
import io.jmix.bpmui.processform.ProcessFormScreens;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.ui.Notifications;
import io.jmix.ui.ScreenTools;
import io.jmix.ui.component.AppWorkArea;
import io.jmix.ui.component.Button;
import io.jmix.ui.component.Window;
import io.jmix.ui.component.mainwindow.Drawer;
import io.jmix.ui.component.mainwindow.SideMenu;
import io.jmix.ui.icon.JmixIcon;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.navigation.Route;
import io.jmix.ui.screen.Screen;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiControllerUtils;
import io.jmix.ui.screen.UiDescriptor;
import org.apache.commons.collections4.CollectionUtils;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@UiController("MainScreen")
@UiDescriptor("main-screen.xml")
@Route(path = "main", root = true)
public class MainScreen extends Screen implements Window.HasWorkArea {

    @Autowired
    private ScreenTools screenTools;

    @Autowired
    private AppWorkArea workArea;
    @Autowired
    private Drawer drawer;
    @Autowired
    private Button collapseDrawerButton;


    @Override
    public AppWorkArea getWorkArea() {
        return workArea;
    }

    @Subscribe("collapseDrawerButton")
    private void onCollapseDrawerButtonClick(Button.ClickEvent event) {
        drawer.toggle();
        if (drawer.isCollapsed()) {
            collapseDrawerButton.setIconFromSet(JmixIcon.CHEVRON_RIGHT);
        } else {
            collapseDrawerButton.setIconFromSet(JmixIcon.CHEVRON_LEFT);
        }
    }

    @Autowired
    private CollectionContainer<ProcessDefinitionData> processDefinitionsDc;

    @Autowired
    private CurrentUserSubstitution currentUserSubstitution;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    private FlowableEntitiesConverter entitiesConverter;

    @Autowired
    private ProcessFormScreens processFormScreens;

    @Autowired
    private SideMenu sideMenu;

    @Autowired
    private Notifications notifications;

    @Autowired
    private Messages messages;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        screenTools.openDefaultScreen(
                UiControllerUtils.getScreenContext(this).getScreens());

        screenTools.handleRedirect();

        createStartProcessMenuItems();
    }

    private void createStartProcessMenuItems() {
        fillProcessDefinitionsDc();

        SideMenu.MenuItem startProcessMenu = sideMenu.createMenuItem("startProcess", "Start Process");
        startProcessMenu.setIcon(JmixIcon.PLAY.source());
        sideMenu.addMenuItem(startProcessMenu, 0);

        processDefinitionsDc.getItems().forEach(processDefinitionData -> {
            SideMenu.MenuItem processStarter = sideMenu.createMenuItem("start" + processDefinitionData.getKey(),
                    "start" + processDefinitionData.getKey(), null,
                    menuItem -> onStartProcess(processDefinitionData));
            startProcessMenu.addChildItem(processStarter);
        });
    }

    private void fillProcessDefinitionsDc() {
        List<ProcessDefinitionData> processDefinitionData = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .active()
                .orderByProcessDefinitionName()
                .asc()
                .list().stream()
                .map(entitiesConverter::createProcessDefinitionData)
                .collect(Collectors.toList());
        processDefinitionsDc.setItems(processDefinitionData);
    }

    public void onStartProcess(ProcessDefinitionData processDefinitionData) {
        String processDefinitionKey = processDefinitionData.getKey();
        ProcessDefinition latestProcessDefinition = findLatestProcessDefinition(processDefinitionKey);

        if (latestProcessDefinition == null) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messages.formatMessage("startProcessScreen.processDefinitionNotFound", processDefinitionKey))
                    .show();
            return;
        }

        if (currentAuthentication.isSet()) Authentication.setAuthenticatedUserId(currentAuthentication.getUser().getUsername());
        processFormScreens.createStartProcessForm(latestProcessDefinition, this)
                .show();
    }

    @Nullable
    protected ProcessDefinition findLatestProcessDefinition(String processDefinitionKey) {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .active()
                .list();
        return !CollectionUtils.isEmpty(processDefinitions) ? processDefinitions.get(0) : null;
    }
}