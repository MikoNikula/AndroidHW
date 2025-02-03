package com.example.androidhw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidhw.ui.theme.AndroidHWTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import android.content.Context
import androidx.compose.ui.graphics.painter.Painter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidHWTheme {
                val navController = rememberNavController()
                val context = this@MainActivity
                var imageUri by remember { mutableStateOf(loadImageUri(context)) }
                var name by remember { mutableStateOf(loadUserName(context)) }

                NavHost(
                    navController = navController,
                    startDestination = "conversation_view"
                ) {
                    composable("conversation_view") { ConversationView(navController, name, imageUri) }
                    composable("settings_view") { SettingsView(navController, context, name, imageUri, onNameChange = {
                        name = it
                        saveUserName(context, it)
                    }, onImageChange = {
                        imageUri = it
                        saveImageUri(context, it)
                    }) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationView(navController: NavController, userName: String, imageUri: Uri?) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings_view") }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Conversation(messages = SampleData.conversationSample, userName = userName, imageUri = imageUri)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView( navController: NavController,
                  context: Context,
                  name: String,
                  imageUri: Uri?,
                  onNameChange: (String) -> Unit,
                  onImageChange: (Uri) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onImageChange(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            } ?: run {
                Image(
                    painter = painterResource(id = R.drawable.profile_picture),
                    contentDescription = "Default Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            BasicTextField(
                value = name,
                onValueChange = { onNameChange(it) },
                modifier = Modifier.padding(8.dp),
                textStyle = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

fun saveImageUri(context: Context, uri: Uri) {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val file = File(context.filesDir, "profile_image.jpg")

    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        sharedPref.edit().putString("image_uri", file.absolutePath).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadImageUri(context: Context): Uri? {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val path = sharedPref.getString("image_uri", null)

    return path?.let { Uri.fromFile(File(it)) }
}

fun saveUserName(context: Context, name: String) {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    sharedPref.edit().putString("user_name", name).apply()
}

fun loadUserName(context: Context): String {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return sharedPref.getString("user_name", "User") ?: "User"
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message, userName: String? = null, imageUri: Uri? = null) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        // Use updated profile picture if available; otherwise, use default
        val painter: Painter = imageUri?.let { rememberAsyncImagePainter(it) }
            ?: painterResource(R.drawable.profile_picture)

        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        var isExpanded by remember { mutableStateOf(false) }
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            // Use updated user name if available; otherwise, use message's author
            Text(
                text = userName ?: msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun Conversation(messages: List<Message>, userName: String, imageUri: Uri?) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(msg = message, userName = userName, imageUri = imageUri)
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    AndroidHWTheme {
        //Conversation(SampleData.conversationSample)
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)

@Composable
fun PreviewMessageCard() {
    AndroidHWTheme {
        Surface {
            MessageCard(
                msg = Message("Lexi", "Hey, take a look at Jetpack Compose, it's great!")
            )
        }
    }
}