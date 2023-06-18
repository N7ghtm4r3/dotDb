package com.tecknobit.dotdb;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;

public class dotDbWindow implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        dotDbContent content = new dotDbContent();
        Content iContent = getInstance().createContent(content.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(iContent);
    }

    private static class dotDbContent {

        private final JPanel contentPanel = new JPanel();


        public JPanel getContentPanel() {
            return contentPanel;
        }

    }

}
