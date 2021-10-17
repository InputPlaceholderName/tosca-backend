package com.github.insertplaceholdername.tosca.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object WaitingLists : IntIdTable("waiting_lists") {
    val workspace = reference("workspace", Workspaces)
    val title = text("title")
    val information = text("information")
}

class WaitingListDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WaitingListDAO>(WaitingLists)

    var workspace by WaitingLists.workspace
    var title by WaitingLists.title
    var information by WaitingLists.information

    fun toModel() = WaitingList(
        id = id.value,
        workspace = workspace.value,
        title = title,
        information = information
    )
}

@Serializable
data class WaitingList(val id: Int, val workspace: Int, val title: String, val information: String)

fun Iterable<WaitingListDAO>.toModelList(): List<WaitingList> = map(WaitingListDAO::toModel).toList()
