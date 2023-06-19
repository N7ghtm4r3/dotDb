package com.tecknobit.dotdb;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;
import static com.tecknobit.dotdb.dotDbWindow.dotDbContent;
import static com.tecknobit.dotdb.dotDbWindow.toolWindow;
import static java.util.List.of;

public class dotDbExecutor extends AnAction {

    private static final ArrayList<String> SUPPORTED_EXTENSIONS = new ArrayList<>(of("sqlite", "sqlite3", "db", "db3",
            "s3db", "sl3"));

    /**
     * Implement this method to provide your action handler.
     *
     * @param e Carries information on the invocation place
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(VIRTUAL_FILE);
        if (file != null && toolWindow != null) {
            if (SUPPORTED_EXTENSIONS.contains(file.getExtension())) {
                try {
                    ContentManager contentManager = toolWindow.getContentManager();
                    contentManager.removeAllContents(true);
                    contentManager.addContent(getInstance().createContent(new dotDbContent(file.getPath()).getContentPanel(),
                            "", false));
                    toolWindow.show();
                } catch (Exception ignored) {
                }
            } else
                showErrorDialog("The file inserted is not supported yet or is not a database", "Wrong File Inserted");
        }
    }

}
