SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[t1](
    [c2] [int] NOT NULL,
    [c1] [int] NOT NULL)
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE VIEW [dbo].[v1] AS
    SELECT * FROM [dbo].[t1]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE VIEW [dbo].[v2] AS
    SELECT * FROM [dbo].[v1]
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE VIEW [dbo].[v3] AS
    SELECT * FROM [dbo].[v2]
GO