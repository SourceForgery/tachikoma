package com.sourceforgery.tachikoma.users

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.exceptions.NotFoundException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUser
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.GetUsersRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ModifyUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ModifyUserResponse
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserResponse
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
    override fun addFrontendUser(request: AddUserRequest, responseObserver: StreamObserver<ModifyUserResponse>) {
        try {
            authentication.requireFrontendAdmin(MailDomain(request.mailDomain))
            val user = userService.addFrontendUser(request)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getFrontendUsers(request: GetUsersRequest, responseObserver: StreamObserver<FrontendUser>) {
        try {
            authentication.requireFrontend()
            userService.getFrontendUsers(authentication.mailDomain, responseObserver)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun modifyFrontendUser(request: ModifyUserRequest, responseObserver: StreamObserver<ModifyUserResponse>) {
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.authId.id))
                ?: throw NotFoundException()
            if (auth.id.authenticationId == request.authId.id) {
                throw IllegalArgumentException("Cannot modify the same account")
            }
            authentication.requireFrontendAdmin(auth.account.mailDomain)
            val user = userService.modifyFrontendUser(request, auth)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun removeUser(request: RemoveUserRequest, responseObserver: StreamObserver<RemoveUserResponse>) {
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.userToRemove.id))
                ?: throw NotFoundException()
            authentication.requireFrontendAdmin(auth.account.mailDomain)
            val user = userService.removeUser(request)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}