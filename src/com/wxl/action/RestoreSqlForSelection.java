package com.wxl.action;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.wxl.Icons;
import com.wxl.util.ConfigUtil;
import com.wxl.util.PrintUtil;
import com.wxl.util.RestoreSqlUtil;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.Objects;

public class RestoreSqlForSelection extends AnAction {
	private static String preparingLine = "";
	private static String parametersLine = "";
	private static boolean isEnd = false;

	public RestoreSqlForSelection() {
		super((String)null, (String)null, Icons.MyBatisIcon);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getProject();
		if (project != null) {
			CaretModel caretModel = Objects.requireNonNull(e.getData(LangDataKeys.EDITOR)).getCaretModel();
			Caret currentCaret = caretModel.getCurrentCaret();
			String sqlText = currentCaret.getSelectedText();
			ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("MyBatis Log");
			if (!ConfigUtil.active || !toolWindow.isAvailable()) {
				(new ShowLogInConsoleAction(project)).showLogInConsole(project);
			}

			assert toolWindow != null;
			toolWindow.activate((Runnable)null);
			String preparing = ConfigUtil.getPreparing(project);
			String parameters = ConfigUtil.getParameters(project);
			if (StringUtils.isNotBlank(sqlText) && sqlText.contains(preparing) && sqlText.contains(parameters)) {
				String[] sqlArr = sqlText.split("\n");
				if (sqlArr.length >= 2) {
					for(int i = 0; i < sqlArr.length; ++i) {
						String currentLine = sqlArr[i];
						if (!StringUtils.isBlank(currentLine)) {
							if (currentLine.contains(preparing)) {
								preparingLine = currentLine;
							} else {
								currentLine = currentLine + "\n";
								if (!StringUtils.isEmpty(preparingLine)) {
									if (currentLine.contains(parameters)) {
										parametersLine = currentLine;
									} else {
										if (StringUtils.isBlank(parametersLine)) {
											continue;
										}

										parametersLine = parametersLine + currentLine;
									}

									if (!parametersLine.endsWith("Parameters: \n") && !parametersLine.endsWith("null\n") && !RestoreSqlUtil.endWithAssembledTypes(parametersLine)) {
										if (i == sqlArr.length - 1) {
											PrintUtil.println(project, "--  Sql Extract  ==>", ConsoleViewContentType.USER_INPUT);
											PrintUtil.println(project, "Can't Sql Extract.", PrintUtil.getOutputAttributes((Color)null, Color.yellow));
											PrintUtil.println(project, "-- ---------------------------------------------------------------------------------------------------------------------", ConsoleViewContentType.USER_INPUT);
											this.reset();
											break;
										}
									} else {
										isEnd = true;
										if (StringUtils.isNotEmpty(preparingLine) && StringUtils.isNotEmpty(parametersLine) && isEnd) {
											String preStr = "--  Sql Extract  - ==>";
											PrintUtil.println(project, preStr, ConsoleViewContentType.USER_INPUT);
											String restoreSql = RestoreSqlUtil.restoreSql(preparingLine, parametersLine);
											PrintUtil.println(project, restoreSql, PrintUtil.getOutputAttributes((Color)null, new Color(255, 200, 0)));
											PrintUtil.println(project, "-- ---------------------------------------------------------------------------------------------------------------------", ConsoleViewContentType.USER_INPUT);
											this.reset();
										}
									}
								}
							}
						}
					}
				} else {
					PrintUtil.println(project, "--  Sql Extract  ==>", ConsoleViewContentType.USER_INPUT);
					PrintUtil.println(project, "Can't Sql Extract.", PrintUtil.getOutputAttributes(null, Color.yellow));
					PrintUtil.println(project, "-- ---------------------------------------------------------------------------------------------------------------------", ConsoleViewContentType.USER_INPUT);
					this.reset();
				}
			} else {
				PrintUtil.println(project, "--  Sql Extract  ==>", ConsoleViewContentType.USER_INPUT);
				PrintUtil.println(project, "Can't Sql Extract.", PrintUtil.getOutputAttributes((Color)null, Color.yellow));
				PrintUtil.println(project, "-- ---------------------------------------------------------------------------------------------------------------------", ConsoleViewContentType.USER_INPUT);
				this.reset();
			}

		}
	}

	private void reset() {
		preparingLine = "";
		parametersLine = "";
		isEnd = false;
	}
}
