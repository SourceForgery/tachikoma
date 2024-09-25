package common

import "context"

type AddAuth = func(ctx context.Context) (authedContext context.Context)
