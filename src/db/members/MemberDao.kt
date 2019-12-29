package db.members

import io.jsondb.JsonDBTemplate
import utils.JsonDbGetter

class MemberDao {

    companion object {
        private const val DB_FILES_LOCATION = "./db"
        private const val BASE_SCAN_PACKAGE = "db.members"
    }

    private val jsonDb = getJsonDb()

    private fun getJsonDb(): JsonDBTemplate {
        return JsonDbGetter.get<Member>(DB_FILES_LOCATION, BASE_SCAN_PACKAGE)
    }

    fun clearAndInsertMembers(members: List<Member>) {
        jsonDb.dropCollection(Member::class.java)
        jsonDb.createCollection(Member::class.java)
        jsonDb.insert(members, Member::class.java)
    }

    fun getMembers(): List<Member> {
        return jsonDb.getCollection(Member::class.java)
    }

    fun getMember(id: String): Member {
        return jsonDb.findById<Member>(id, Member::class.java)
    }

    fun getMemberByNickname(nickname: String): Member {
        return getMembers().first { it.profile!!.displayName == nickname }
    }
}