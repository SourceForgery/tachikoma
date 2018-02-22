package com.sourceforgery.tachikoma.users

import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserResponse
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UpdateUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.User
import io.grpc.stub.StreamObserver
import javax.inject.Inject

class UserService
@Inject
private constructor(

) {
    fun addUser(request: AddUserRequest): User {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun modifyUser(request: UpdateUserRequest): User? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getUsers(mailDomain: String, responseObserver: StreamObserver<User>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun removeUser(request: RemoveUserRequest): RemoveUserResponse {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
