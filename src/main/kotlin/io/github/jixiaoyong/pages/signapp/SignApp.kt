package io.github.jixiaoyong.pages.signapp

import ApkSigner
import LocalWindow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import io.github.jixiaoyong.beans.CommandResult
import io.github.jixiaoyong.beans.SignType
import io.github.jixiaoyong.pages.Routes
import io.github.jixiaoyong.utils.FileChooseUtil
import io.github.jixiaoyong.utils.showToast
import io.github.jixiaoyong.widgets.ButtonWidget
import io.github.jixiaoyong.widgets.HoverableTooltip
import io.github.jixiaoyong.widgets.InfoItemWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.util.*
import javax.swing.JPanel

/**
 * @author : jixiaoyong
 * @description ：签名app的地方
 * 1. 选择/拖拽APP
 * 2. 开始签名/查看签名
 * 3. 签名历史
 *
 * 自动匹配apk签名的逻辑：
 * 1. 当前只选中了一个apk
 * 2. apk在apkSignatureMap中有对应的签名，并且该签名在signInfoBeans中有效
 *
 * @email : jixiaoyong1995@gmail.com
 * @date : 2023/8/18
 */

@Composable
fun PageSignApp(
    viewModel: SignAppViewModel,
    onChangePage: (String) -> Unit
) {
    val window = LocalWindow.current
    val clipboard = LocalClipboardManager.current

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val uiState by viewModel.uiState.collectAsState()

    val currentApkFilePath = uiState.apkFilePaths

    val local = uiState.signInfoResult
    when (local) {
        is CommandResult.Success<*> -> {
            Popup(onDismissRequest = {
                viewModel.changeSignInfo(CommandResult.NOT_EXECUT)
            }, alignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(0.56f))
                        .padding(horizontal = 50.dp, vertical = 65.dp)
                        .background(Color.White, shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 20.dp, vertical = 15.dp)
                ) {
                    Text(
                        "签名信息（鼠标上下滚动查看更多）",
                        color = MaterialTheme.colors.onSurface,
                        fontWeight = FontWeight.W800,
                        modifier = Modifier.padding(20.dp).align(alignment = Alignment.CenterHorizontally)
                    )
                    Column(
                        modifier = Modifier.weight(1f, fill = false).heightIn(max = 450.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                local.result?.toString() ?: "",
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                    TextButton(onClick = {
                        viewModel.changeSignInfo(CommandResult.NOT_EXECUT)
                    }, modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                        Text(
                            "确认",
                            color = Color(0xff007AFF)
                        )
                    }
                }
            }
        }

        is CommandResult.Error<*> -> {
            scope.launch {
                showToast("查询签名失败:${local.message}")
            }
        }

        else -> {}
    }

    if (local is CommandResult.EXECUTING) {
        Popup(alignment = Alignment.Center) {
            Box(
                modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colors.onBackground.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.size(150.dp)
                        .background(color = Color.Black.copy(0.8f), shape = RoundedCornerShape(5.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(80.dp).padding(10.dp))
                    Text("处理中……", color = Color.White.copy(0.8f))
                }
            }
        }
    }

    Scaffold(scaffoldState = scaffoldState) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DropBoxPanel(
                    window,
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(10.dp)
                        .background(color = Color(0xffF2F2F7), shape = RoundedCornerShape(10.dp))
                        .padding(15.dp)
                        .clickable {
                            scope.launch {
                                val chooseFileName =
                                    FileChooseUtil.chooseMultiFile(
                                        window,
                                        "请选择要签名的apk文件",
                                        filter = { _, name ->
                                            name.toLowerCase(Locale.getDefault()).endsWith(".apk")
                                        })
                                if (chooseFileName.isNullOrEmpty()) {
                                    showToast("请选择要签名的apk文件")
                                } else {
                                    if (!uiState.signedOutputDirectory.isNullOrBlank()) {
                                        viewModel.saveSignedOutputDirectory(
                                            chooseFileName.first().substringBeforeLast(File.separator)
                                        )
                                    }
                                    viewModel.changeApkFilePath(chooseFileName)
                                    showToast("修改成功")
                                }
                            }
                        },
                    component = JPanel(),
                    onFileDrop = {
                        scope.launch {
                            val file = it.filter() {
                                it.lowercase(Locale.getDefault()).endsWith(".apk")
                            }
                            if (file.isEmpty()) {
                                showToast("请先选择正确的apk文件")
                            } else {
                                viewModel.changeApkFilePath(file)
                                showToast("修改成功")
                            }
                        }
                    }
                ) {
                    Text(
                        text = "请拖拽apk文件到这里\n(支持多选，也可以点击这里选择apk文件)",
                        textAlign = TextAlign.Center,
                        color = Color(0xFFBABEBE),
                        modifier = Modifier.align(alignment = Alignment.Center)
                    )
                }
                InfoItemWidget(
                    "当前选择的文件${if (currentApkFilePath.isEmpty()) "" else "(" + currentApkFilePath.size + ")"}",
                    if (currentApkFilePath.isEmpty()) "请先选择apk文件" else currentApkFilePath.joinToString("\n"),
                    buttonTitle = "查看签名",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (currentApkFilePath.isEmpty()) {
                                showToast("请先选择apk文件")
                            } else {
                                viewModel.changeSignInfo(CommandResult.EXECUTING)
                                val resultList = currentApkFilePath.map { ApkSigner.getApkSignInfo(it) }
                                val mergedResult = viewModel.mergeCommandResult(resultList, currentApkFilePath)
                                viewModel.changeSignInfo(mergedResult)
                            }
                        }
                    }
                )
                InfoItemWidget(
                    "当前的签名文件",
                    uiState.currentSignInfo?.toString() ?: "暂无",
                    onClick = {
                        onChangePage(Routes.SignInfo)
                        viewModel.removeApkSignature(uiState.apkPackageName)
                    })

                val errorTips = "请先选择签名文件输出目录"
                InfoItemWidget(
                    "签名后的文件输出目录",
                    uiState.signedOutputDirectory ?: errorTips,
                    buttonTitle = "修改目录",
                    onClick = {
                        scope.launch {
                            val outputDirectory =
                                FileChooseUtil.chooseSignDirectory(
                                    window,
                                    errorTips,
                                    uiState.signedOutputDirectory ?: currentApkFilePath.firstOrNull()
                                )
                            if (outputDirectory.isNullOrBlank()) {
                                showToast(errorTips)
                            } else {
                                viewModel.saveSignedOutputDirectory(outputDirectory)
                                showToast("修改成功")
                            }
                        }
                    }
                )

                InfoItemWidget(
                    "签名方案",
                    null,
                    showChangeButton = false
                ) {
                    Row {
                        SignType.ALL_SIGN_TYPES.forEachIndexed { index, item ->
                            val isSelected = uiState.apkSignType.contains(item.type)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isSelected,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xff007AFF)),
                                    onCheckedChange = {
                                        val newTypes = mutableSetOf<Int>()
                                        newTypes.addAll(uiState.apkSignType)
                                        if (isSelected) {
                                            newTypes.remove(item.type)
                                        } else {
                                            newTypes.add(item.type)
                                        }

                                        viewModel.changeApkSignType(newTypes)
                                    })
                                Text(item.name, color = Color.Black)
                                HoverableTooltip(description = item.description)
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "是否开启对齐",
                        style = TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onPrimary
                        ),
                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                    )

                    Switch(checked = uiState.isZipAlign,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xff007AFF)),
                        onCheckedChange = {
                            viewModel.changeZipAlign(it)
                        })
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(color = Color.White.copy(0.8f))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val signedButtonEnable = (CommandResult.NOT_EXECUT == uiState.apkSignedResult) &&
                        currentApkFilePath.isNotEmpty()
                ButtonWidget(
                    {
                        scope.launch(Dispatchers.IO) {
                            if (currentApkFilePath.filter { it.lowercase(Locale.getDefault()).endsWith(".apk") }
                                    .isEmpty()
                            ) {
                                showToast("请先选择正确的apk文件")
                                return@launch
                            }

                            val localSelectedSignInfo = uiState.currentSignInfo
                            if (null == localSelectedSignInfo || !localSelectedSignInfo.isValid()) {
                                onChangePage(Routes.SignInfo)
                                showToast("请先配置正确的签名文件")
                                return@launch
                            }

                            if (!ApkSigner.isInitialized()) {
                                onChangePage(Routes.SettingInfo)
                                showToast("请先配置apksigner和zipalign路径")
                                return@launch
                            }

                            if (uiState.apkSignType.isEmpty()) {
                                showToast("请至少选择一种签名方式")
                                return@launch
                            }

                            viewModel.changeSignApkResult(CommandResult.EXECUTING)
                            val signResult = ApkSigner.alignAndSignApk(
                                currentApkFilePath,
                                localSelectedSignInfo.keyStorePath,
                                localSelectedSignInfo.keyAlias,
                                localSelectedSignInfo.keyStorePassword,
                                localSelectedSignInfo.keyPassword,
                                signedApkDirectory = uiState.signedOutputDirectory,
                                zipAlign = uiState.isZipAlign,
                                signVersions = SignType.ALL_SIGN_TYPES.filter {
                                    uiState.apkSignType.contains(it.type)
                                },
                                onProgress = { line ->
                                    scope.launch {
                                        val old = uiState.signedLogs
                                        val newLogs = mutableListOf<String>().apply {
                                            addAll(old)
                                            add(line)
                                        }
                                        viewModel.updateSignedLogs(newLogs)
                                    }
                                }
                            )

                            val mergedResult = viewModel.mergeCommandResult(signResult, currentApkFilePath)
                            viewModel.changeSignApkResult(mergedResult)
                            val firstSuccessSignedApk =
                                signResult.firstOrNull { it is CommandResult.Success<*> } as CommandResult.Success<*>?

                            if (mergedResult is CommandResult.Success<*> && !firstSuccessSignedApk?.result?.toString()
                                    .isNullOrBlank()
                            ) {

                                launch(Dispatchers.IO) {
                                    if (uiState.isAutoMatchSignature && 1 == currentApkFilePath.size) {
                                        //  将当前签名和apk包名关联
                                        viewModel.updateApkSignatureMap(
                                            uiState.apkPackageName,
                                            localSelectedSignInfo,
                                        )
                                    }
                                }

                                val result = scaffoldState.snackbarHostState.showSnackbar(
                                    "签名成功，是否打开签名后的文件？",
                                    "打开",
                                    SnackbarDuration.Long
                                )
                                val file = File(firstSuccessSignedApk?.result?.toString() ?: "")
                                if (SnackbarResult.ActionPerformed == result && file.exists()) {
                                    Desktop.getDesktop().open(file.parentFile)
                                }
                            } else if (mergedResult is CommandResult.Error<*>) {
                                val result = scaffoldState.snackbarHostState.showSnackbar(
                                    "签名失败，：${mergedResult.message}",
                                    "复制错误信息",
                                    SnackbarDuration.Indefinite
                                )
                                if (SnackbarResult.ActionPerformed == result) {
                                    clipboard.setText(AnnotatedString("${mergedResult.message}"))
                                }
                            }
                            viewModel.changeSignApkResult(CommandResult.NOT_EXECUT)
                        }

                    },
                    enabled = signedButtonEnable,
                    title = "开始签名apk",
                    isHighlight = true,
                    modifier = Modifier.size(250.dp, 50.dp),
                )
            }
        }
    }
}
