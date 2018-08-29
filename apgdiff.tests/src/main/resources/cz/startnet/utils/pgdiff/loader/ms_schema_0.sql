SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[fax_boxes] (
	[fax_box_id] [int] NOT NULL,
	[name] [text])
GO

ALTER TABLE [dbo].[fax_boxes] 
    ADD CONSTRAINT [PK_fax_boxes] PRIMARY KEY CLUSTERED  ([fax_box_id]) ON [PRIMARY]
GO

-- TODO uncomment this firstly
--ALTER TABLE [dbo].[fax_boxes] 
--    ADD CONSTRAINT [DF_fax_boxes] DEFAULT (getdate()) FOR [time_received]
--GO

ALTER AUTHORIZATION ON OBJECT::[dbo].[fax_boxes] TO [ms_user];    
GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[faxes] (
    [fax_id] [int] NOT NULL,
    [fax_box_id] [int],
    [from_name] [text],
    [from_number] [text],
    [status] [int],
    [pages] [int],
    [time_received] [datetime],
    [time_finished_received] [datetime],
    [read] [int],
    [station_id] [text])
GO

ALTER TABLE [dbo].[faxes] 
    ADD CONSTRAINT [faxes_pkey] PRIMARY KEY CLUSTERED  ([fax_id]) ON [PRIMARY]
GO

-- TODO uncomment this
--ALTER TABLE [dbo].[faxes] 
--    ADD CONSTRAINT [faxes_fax_box_id_fkey] FOREIGN KEY (fax_box_id) REFERENCES fax_boxes(fax_box_id)
--    ON UPDATE CASCADE 
--    ON DELETE CASCADE
--GO

SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[extensions] (
    [id] [int] NOT NULL)
GO

-- TODO uncomment this 
--ALTER TABLE [dbo].[extensions] 
--    ADD CONSTRAINT [extensions_fax_box_id_fkey] FOREIGN KEY (fax_box_id) REFERENCES fax_boxes(fax_box_id)
--    ON UPDATE NO ACTION
--    ON DELETE SET NULL
--GO  
