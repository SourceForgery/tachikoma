package com.sourceforgery.tachikoma.users

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
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
import com.sourceforgery.tachikoma.grpc.grpcFuture
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

class UserServiceGrpcImpl(
    override val di: DI
) : UserServiceGrpc.UserServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val userService: UserService by instance()
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val authenticationDAO: AuthenticationDAO by instance()

    override fun addFrontendUser(request: AddUserRequest, responseObserver: StreamObserver<ModifyUserResponse>) = grpcFuture(responseObserver) {
        try {
            authentication().requireFrontendAdmin(MailDomain(request.mailDomain))
            val user = userService.addFrontendUser(request)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getFrontendUsers(request: GetUsersRequest, responseObserver: StreamObserver<FrontendUser>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            userService.getFrontendUsers(auth.mailDomain, responseObserver)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun modifyFrontendUser(request: ModifyUserRequest, responseObserver: StreamObserver<ModifyUserResponse>) = grpcFuture(responseObserver) {
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.authId.id))
                ?: throw NotFoundException()
            if (auth.id.authenticationId == request.authId.id) {
                throw IllegalArgumentException("Cannot modify the same account")
            }
            authentication().requireFrontendAdmin(auth.account.mailDomain)
            val user = userService.modifyFrontendUser(request, auth)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun removeUser(request: RemoveUserRequest, responseObserver: StreamObserver<RemoveUserResponse>) = grpcFuture(responseObserver) {
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.userToRemove.id))
                ?: throw NotFoundException()
            authentication().requireFrontendAdmin(auth.account.mailDomain)
            val user = userService.removeUser(request)
            responseObserver.onNext(user)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
