package com.moci.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.json.JSONObject

data class Song(val uri:String,val title:String,val artist:String="未知歌手",val album:String="我的音乐",val lyrics:String="",val favorite:Boolean=false)

class MainActivity: ComponentActivity(){
    private lateinit var player:ExoPlayer
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);player=ExoPlayer.Builder(this).build();setContent{MoCiApp(this,player)}}
    override fun onDestroy(){player.release();super.onDestroy()}
}

@Composable fun MoCiApp(ctx:Context,player:ExoPlayer){
    val prefs=ctx.getSharedPreferences("moci",Context.MODE_PRIVATE)
    var songs by remember{mutableStateOf(loadSongs(prefs))}; var query by remember{mutableStateOf("")}; var current by remember{mutableStateOf<Song?>(null)}; var playing by remember{mutableStateOf(false)}; var edit by remember{mutableStateOf<Song?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->val add=uris.map{u->try{ctx.contentResolver.takePersistableUriPermission(u,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};Song(u.toString(),displayName(ctx.contentResolver,u))};songs=(songs+add).distinctBy{it.uri};saveSongs(prefs,songs)}
    MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFB9A7FF),background=Color(0xFF101014),surface=Color(0xFF19191F))){Scaffold(containerColor=Color(0xFF101014),bottomBar={NavigationBar(containerColor=Color(0xFF15151A)){Text("⌂",Modifier.weight(1f).padding(18.dp));Text("🎵",Modifier.weight(1f).padding(18.dp));Text("♡",Modifier.weight(1f).padding(18.dp))}}){pad->Column(Modifier.padding(pad).fillMaxSize().padding(horizontal=20.dp)){Spacer(Modifier.height(18.dp));Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("陌辞",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("我的音乐，我自己定义",color=Color.Gray)};Text("＋",fontSize=30.sp,modifier=Modifier.clickable{picker.launch(arrayOf("audio/*"))})};Spacer(Modifier.height(18.dp));OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("搜索歌曲、歌手、专辑")});Spacer(Modifier.height(18.dp));Text("我的音乐",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));if(songs.isEmpty())EmptyState{picker.launch(arrayOf("audio/*"))}else LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(songs.filter{query.isBlank()||it.title.contains(query,true)||it.artist.contains(query,true)||it.album.contains(query,true)}){s->SongRow(s,{current=s;player.setMediaItem(MediaItem.fromUri(s.uri));player.prepare();player.play();playing=true},{songs=songs.map{if(it.uri==s.uri)it.copy(favorite=!it.favorite)else it};saveSongs(prefs,songs)},{edit=s})}};current?.let{s->PlayerBar(s,playing){if(player.isPlaying){player.pause();playing=false}else{player.play();playing=true}}}}}}}
    edit?.let{s->EditDialog(s,{u->songs=songs.map{if(it.uri==s.uri)u else it};saveSongs(prefs,songs);edit=null},{edit=null})}
}

@Composable fun EmptyState(onImport:()->Unit){Column(Modifier.fillMaxWidth().padding(50.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("还没有音乐",style=MaterialTheme.typography.titleLarge);Text("把你喜欢的本地歌曲导入陌辞",color=Color.Gray,modifier=Modifier.padding(8.dp));Button(onClick=onImport){Text("导入音乐")}}}
@Composable fun SongRow(s:Song,onPlay:()->Unit,onFav:()->Unit,onEdit:()->Unit){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF19191F)).clickable{onPlay()}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF40375F),Color(0xFF17171D)))),contentAlignment=Alignment.Center){Text("♪",fontSize=25.sp)};Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(s.title,maxLines=1,fontWeight=FontWeight.SemiBold);Text(s.artist,color=Color.Gray,fontSize=13.sp)};Text(if(s.favorite)"♥" else "♡",Modifier.clickable{onFav()}.padding(8.dp));Text("⋯",Modifier.clickable{onEdit()}.padding(8.dp))}}
@Composable fun PlayerBar(s:Song,playing:Boolean,toggle:()->Unit){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF24232B)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF40375F)),contentAlignment=Alignment.Center){Text("♪")};Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(s.title,maxLines=1);Text(s.artist,color=Color.Gray,fontSize=12.sp)};Text(if(playing)"Ⅱ" else "▶",fontSize=22.sp,Modifier.clickable{toggle()}.padding(10.dp))}}
@Composable fun EditDialog(s:Song,onSave:(Song)->Unit,onCancel:()->Unit){var t by remember{mutableStateOf(s.title)};var a by remember{mutableStateOf(s.artist)};var al by remember{mutableStateOf(s.album)};var l by remember{mutableStateOf(s.lyrics)};AlertDialog(onDismissRequest=onCancel,title={Text("编辑歌曲")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(t,{t=it},label={Text("歌曲名称")});OutlinedTextField(a,{a=it},label={Text("歌手")});OutlinedTextField(al,{al=it},label={Text("专辑")});OutlinedTextField(l,{l=it},label={Text("歌词")},minLines=3)}},confirmButton={Button(onClick={onSave(s.copy(title=t,artist=a,album=al,lyrics=l))}){Text("保存")}},dismissButton={TextButton(onClick=onCancel){Text("取消")}})}
fun displayName(cr:ContentResolver,u:Uri):String=u.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?:"未命名歌曲"
fun loadSongs(p:android.content.SharedPreferences):List<Song>{return try{val a=JSONArray(p.getString("songs","[]"));List(a.length()){i->val o=a.getJSONObject(i);Song(o.getString("uri"),o.getString("title"),o.optString("artist","未知歌手"),o.optString("album","我的音乐"),o.optString("lyrics",""),o.optBoolean("favorite",false))}}catch(_:Exception){emptyList()}}
fun saveSongs(p:android.content.SharedPreferences,s:List<Song>){val a=JSONArray();s.forEach{a.put(JSONObject().apply{put("uri",it.uri);put("title",it.title);put("artist",it.artist);put("album",it.album);put("lyrics",it.lyrics);put("favorite",it.favorite)})};p.edit().putString("songs",a.toString()).apply()}
