package com.sourceforgery.tachikoma.users

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.exceptions.NotFoundException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.GetUsersRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserResponse
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UpdateUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.User
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UserServiceGrpc
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.grpc.stub.StreamObserver
import javax.inject.Inject

class UserServiceGrpcImpl
@Inject
private constructor(
        private val userService: UserService,
        private val authentication: Authentication,
        private val grpcExceptionMap: GrpcExceptionMap,
        private val authenticationDAO: AuthenticationDAO
) : UserServiceGrpc.UserServiceImplBase() {
    override fun addUser(request: AddUserRequest, responseObserver: StreamObserver<User>) {
        try {
            authentication.requireFrontendAdmin(MailDomain(request.mailDomain))
            val user = userService.addUser(request)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getUsers(request: GetUsersRequest, responseObserver: StreamObserver<User>) {
        try {
            authentication.requireFrontend()
            userService.getUsers(authentication.mailDomain.mailDomain, responseObserver)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun modifyUser(request: UpdateUserRequest, responseObserver: StreamObserver<User>) {
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.authId.id))
                    ?: throw NotFoundException()
            authentication.requireFrontendAdmin(auth.account.mailDomain)
            val user = userService.modifyUser(request)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun removeUser(request: RemoveUserRequest, responseObserver: StreamObserver<RemoveUserResponse>) {
        val auth = authenticationDAO.getById(AuthenticationId(request.userToRemove.id))
                ?: throw NotFoundException()
        authentication.requireFrontendAdmin(auth.account.mailDomain)
        val user = userService.removeUser(request)
        responseObserver.onNext(user)
        responseObserver.onCompleted()
    }
}