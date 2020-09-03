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
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UserServiceGrpcKt
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

class UserServiceGrpcImpl(
    override val di: DI
) : UserServiceGrpcKt.UserServiceCoroutineImplBase(), DIAware {

    private val userService: UserService by instance()
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val authenticationDAO: AuthenticationDAO by instance()

    override suspend fun addFrontendUser(request: AddUserRequest): ModifyUserResponse =
        try {
            authentication().requireFrontendAdmin(MailDomain(request.mailDomain))
            userService.addFrontendUser(request)
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }

    override fun getFrontendUsers(request: GetUsersRequest): Flow<FrontendUser> = flow<FrontendUser> {
        val auth = authentication()
        auth.requireFrontend()
        userService.getFrontendUsers(auth.mailDomain)
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }

    override suspend fun modifyFrontendUser(request: ModifyUserRequest): ModifyUserResponse =
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.authId.id))
                ?: throw NotFoundException()
            if (auth.id.authenticationId == request.authId.id) {
                throw IllegalArgumentException("Cannot modify the same account")
            }
            authentication().requireFrontendAdmin(auth.account.mailDomain)
            userService.modifyFrontendUser(request, auth)
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }

    override suspend fun removeUser(request: RemoveUserRequest): RemoveUserResponse =
        try {
            val auth = authenticationDAO.getById(AuthenticationId(request.userToRemove.id))
                ?: throw NotFoundException()
            authentication().requireFrontendAdmin(auth.account.mailDomain)
            userService.removeUser(request)
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
}
