package com.gj.arcoredraw

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import kotlinx.android.synthetic.main.activity_cross.*
import kotlinx.android.synthetic.main.activity_cross.UI_ArSceneView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.sqrt

class CrossActivity : AppCompatActivity() {

    private var units=true
    private val dataArray = arrayListOf<AnchorInfoBean>()
    private val lineNodeArray = arrayListOf<Node>()
    private val sphereNodeArray = arrayListOf<Node>()
    private val startNodeArray = arrayListOf<Node>()
    private val endNodeArray = arrayListOf<Node>()

    private lateinit var startNode: AnchorNode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )// 设置全屏
        setContentView(R.layout.activity_cross)
        initView()
    }

    private fun initView() {
        UI_Last2.setOnClickListener{
            //上一步
            when (dataArray.size) {
                0 -> {
                    ToastUtils.showLong("没有操作记录")
                }
                1 -> {
                    dataArray.clear()
                    lineNodeArray.clear()
                    sphereNodeArray.clear()
                    startNodeArray.clear()
                    endNodeArray.clear()
                    (UI_ArSceneView as MyArFragment).arSceneView.scene.removeChild(startNode)
                }
                else -> {
                    dataArray.removeAt(dataArray.size - 1)
                    val index = startNodeArray.size - 1
                    startNodeArray[index].removeChild(lineNodeArray.removeAt(index))
                    endNodeArray[index].removeChild(sphereNodeArray.removeAt(index + 1))
                    (UI_ArSceneView as MyArFragment).arSceneView.scene.removeChild(startNodeArray.removeAt(index))
                    (UI_ArSceneView as MyArFragment).arSceneView.scene.removeChild(endNodeArray.removeAt(index))
                }
            }
        }

        UI_Post2.setOnClickListener{
            units = units != true
        }
        initAr()
    }

    private fun initAr() {
        (UI_ArSceneView as MyArFragment).setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            val anchorInfoBean = AnchorInfoBean("", hitResult.createAnchor(), 0.0)
            dataArray.add(anchorInfoBean)

            if (dataArray.size == 2) {
                val endAnchor = dataArray[dataArray.size - 1].anchor
                val startAnchor = dataArray[dataArray.size - 2].anchor

                val startPose = endAnchor.pose
                val endPose = startAnchor.pose
                val dx = startPose.tx() - endPose.tx()
                val dy = startPose.ty() - endPose.ty()
                val dz = startPose.tz() - endPose.tz()

                anchorInfoBean.length = sqrt((dx * dx + dy * dy + dz * dz).toDouble())

                drawLine(startAnchor, endAnchor, anchorInfoBean.length)
            } else if (dataArray.size == 1) {
                startNode = AnchorNode(hitResult.createAnchor())
                startNode.setParent((UI_ArSceneView as MyArFragment).arSceneView.scene)
                MaterialFactory.makeOpaqueWithColor(this@CrossActivity, com.google.ar.sceneform.rendering.Color(0.33f, 0.87f, 0f))
                    .thenAccept { material ->
                        val sphere = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material)
                        sphereNodeArray.add(    Node().apply {
                            setParent(startNode)
                            localPosition = Vector3.zero()
                            renderable = sphere
                        })
                    }
            } else {
                ToastUtils.showShort("目前就只能点两个点")
            }
        }
    }

    private fun drawLine(firstAnchor: Anchor, secondAnchor: Anchor, length: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val firstAnchorNode = AnchorNode(firstAnchor)
            startNodeArray.add(firstAnchorNode)

            val secondAnchorNode = AnchorNode(secondAnchor)
            endNodeArray.add(secondAnchorNode)

            firstAnchorNode.setParent((UI_ArSceneView as MyArFragment).arSceneView.scene)
            secondAnchorNode.setParent((UI_ArSceneView as MyArFragment).arSceneView.scene)

            MaterialFactory.makeOpaqueWithColor(
                this@CrossActivity,
                com.google.ar.sceneform.rendering.Color(0.53f, 0.92f, 0f)
            )
                .thenAccept { material ->
                    val sphere = ShapeFactory.makeSphere(0.02f, Vector3(0.0f, 0.0f, 0.0f), material)
                    sphereNodeArray.add(Node().apply {
                        setParent(secondAnchorNode)
                        localPosition = Vector3.zero()
                        renderable = sphere
                    })
                }

            val firstWorldPosition = firstAnchorNode.worldPosition
            val secondWorldPosition = secondAnchorNode.worldPosition

            val difference = Vector3.subtract(firstWorldPosition, secondWorldPosition)
            val directionFromTopToBottom = difference.normalized()
            val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())

            MaterialFactory.makeOpaqueWithColor(
                this@CrossActivity,
                com.google.ar.sceneform.rendering.Color(0.33f, 0.87f, 0f)
            )
                .thenAccept { material ->
                    val lineMode = ShapeFactory.makeCube(
                        Vector3(0.01f, 0.01f, difference.length()),
                        Vector3.zero(),
                        material
                    )
                    val lineNode = Node().apply {
                        setParent(firstAnchorNode)
                        renderable = lineMode
                        worldPosition =
                            Vector3.add(firstWorldPosition, secondWorldPosition).scaled(0.5f)
                        worldRotation = rotationFromAToB
                    }
                    lineNodeArray.add(Node().apply {
                        setParent(firstAnchorNode)
                        renderable = lineMode
                        worldPosition =
                            Vector3.add(firstWorldPosition, secondWorldPosition).scaled(0.5f)
                        worldRotation = rotationFromAToB
                    })

                    ViewRenderable.builder()
                        .setView(this@CrossActivity, R.layout.renderable_text)
                        .build()
                        .thenAccept { it ->
                            if(units){
                                (it.view as TextView).text = "${String.format("%.1f", length * 100)}CM"
                            }else{
                                (it.view as TextView).text = "${String.format("%.1f", length * 100 / 2.54)}IN"
                            }

                            it.isShadowCaster = false
                            FaceToCameraNode().apply {
                                setParent(lineNode)
                                localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
                                localPosition = Vector3(0f, 0.02f, 0f)
                                renderable = it
                            }
                        }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (UI_ArSceneView as MyArFragment).onDestroy()
    }
}