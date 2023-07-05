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

/**
 * The {@code dotDbExecutor} class is useful to execute the {@code dotDb}'s plugin
 *
 * @author N7ghtm4r3 - Tecknobit
 * @apiNote you can use the macro-shortcut {@code Maiusc + G} or the right-click on the database file you want to choose
 * and the {@link dotDbWindow} will show the {@link dotDbContent}
 * @see AnAction
 */
public class dotDbExecutor extends AnAction {

    /**
     * {@code SUPPORTED_EXTENSIONS} list of supported database file extensions
     */
    private static final ArrayList<String> SUPPORTED_EXTENSIONS = new ArrayList<>(of("sqlite", "sqlite3", "db", "db3",
            "s3db", "sl3"));

    /**
     * Implement this method to provide your action handler.
     *
     * @param e Carries information on the invocation place
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (toolWindow == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        VirtualFile file = e.getData(VIRTUAL_FILE);
        if (file != null) {
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
