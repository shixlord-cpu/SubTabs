package de.sasbe.subtabs;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class SubtabGroupProjectViewNode extends ProjectViewNode<SubtabGroupProjectViewNode.GroupValue> {
    record GroupValue(@NotNull String displayName, @NotNull VirtualFile primaryFile) {
    }

    private final List<PsiFileNode> fileNodes;

    SubtabGroupProjectViewNode(
            @NotNull Project project,
            @NotNull String groupKey,
            @NotNull List<PsiFileNode> fileNodes,
            @NotNull ViewSettings settings
    ) {
        super(
                project,
                new GroupValue(
                        ComponentFileNaming.displayName(groupKey),
                        primaryFile(fileNodes)
                ),
                settings
        );
        this.fileNodes = List.copyOf(fileNodes);
    }

    @Override
    public @NotNull List<AbstractTreeNode<?>> getChildren() {
        return new ArrayList<>(fileNodes);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(getValue().displayName());

        PsiFileNode primaryNode = fileNodes.get(0);
        PresentationData primaryPresentation = new PresentationData();
        primaryNode.update(primaryPresentation);
        if (primaryPresentation.getIcon(false) != null) {
            presentation.setIcon(primaryPresentation.getIcon(false));
        }
        if (fileNodes.size() > 1) {
            presentation.setLocationString(fileNodes.size() + " Dateien");
        }
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        for (PsiFileNode fileNode : fileNodes) {
            if (file.equals(fileNode.getVirtualFile())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return true;
    }

    @Override
    public void navigate(boolean requestFocus) {
        Project project = getProject();
        if (project == null) {
            fileNodes.get(0).navigate(requestFocus);
            return;
        }

        List<VirtualFile> groupFiles = groupFiles();
        SubtabGroupNavigation.navigateGroup(
                project,
                groupFiles,
                requestFocus,
                () -> fileNodes.get(0).navigate(requestFocus)
        );
    }

    private @NotNull List<VirtualFile> groupFiles() {
        List<VirtualFile> files = new ArrayList<>(fileNodes.size());
        for (PsiFileNode fileNode : fileNodes) {
            VirtualFile file = fileNode.getVirtualFile();
            if (file != null) {
                files.add(file);
            }
        }
        return files;
    }

    @Override
    public boolean expandOnDoubleClick() {
        return false;
    }

    @Override
    public @Nullable VirtualFile getVirtualFile() {
        return getValue().primaryFile();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SubtabGroupProjectViewNode groupNode)) {
            return false;
        }
        return getValue().primaryFile().equals(groupNode.getValue().primaryFile());
    }

    @Override
    public int hashCode() {
        return getValue().primaryFile().hashCode();
    }

    private static @NotNull VirtualFile primaryFile(@NotNull List<PsiFileNode> fileNodes) {
        VirtualFile file = fileNodes.get(0).getVirtualFile();
        if (file == null) {
            throw new IllegalArgumentException("Grouped file node has no virtual file");
        }
        return file;
    }
}
