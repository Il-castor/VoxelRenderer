package com.example.voxelrenderer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Schermata iniziale: elenca tutti i file .vly presenti in assets/ e, alla
 * selezione, apre RendererActivity passando il nome del file scelto.
 */
class FileListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vlyFiles = listVlyAssets()

        setContent {
            MaterialTheme {
                FileListScreen(
                    files = vlyFiles,
                    onFileSelected = { fileName -> openRenderer(fileName) }
                )
            }
        }
    }

    /** Elenca i file con estensione .vly presenti nella cartella assets/. */
    private fun listVlyAssets(): List<String> {
        val all = assets.list("") ?: emptyArray()
        return all.filter { it.endsWith(".vly", ignoreCase = true) }.sorted()
    }

    private fun openRenderer(fileName: String) {
        val intent = Intent(this, RendererActivity::class.java)
        intent.putExtra(RendererActivity.EXTRA_VLY_FILE_NAME, fileName)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(files: List<String>, onFileSelected: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Voxel Renderer") })
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nessun file .vly trovato in assets/")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(files) { fileName ->
                VlyFileRow(fileName = fileName, onClick = { onFileSelected(fileName) })
                Divider()
            }
        }
    }
}

@Composable
fun VlyFileRow(fileName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = fileName, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}