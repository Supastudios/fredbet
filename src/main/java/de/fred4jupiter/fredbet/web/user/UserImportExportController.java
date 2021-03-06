package de.fred4jupiter.fredbet.web.user;

import de.fred4jupiter.fredbet.security.FredBetPermission;
import de.fred4jupiter.fredbet.service.excel.ExcelReadingException;
import de.fred4jupiter.fredbet.service.user.UserImportExportService;
import de.fred4jupiter.fredbet.web.WebMessageUtil;
import de.fred4jupiter.fredbet.web.admin.ExcelImportController;
import de.fred4jupiter.fredbet.web.admin.ExcelUploadCommand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/user")
@PreAuthorize("hasAuthority('" + FredBetPermission.PERM_ADMINISTRATION + "')")
public class UserImportExportController {

    private static final Logger LOG = LoggerFactory.getLogger(UserImportExportController.class);

    private static final String CONTENT_TYPE_JSON = "application/json";

    @Autowired
    private UserImportExportService userImportExportService;

    @Autowired
    private WebMessageUtil messageUtil;

    private static final String REDIRECT_SHOW_PAGE = "redirect:/user/importexport";

    @ModelAttribute("userImportExportCommand")
    public UserImportExportCommand initCommand() {
        return new UserImportExportCommand();
    }

    @GetMapping("/importexport")
    public String showUploadPage() {
        return "user/user_import_export";
    }

    @GetMapping(value = "/export", produces = CONTENT_TYPE_JSON)
    public ResponseEntity<byte[]> exportUsers() {
        final String fileName = "all_users.json";
        String json = this.userImportExportService.exportAllUsersToJson();
        if (StringUtils.isBlank(json)) {
            return ResponseEntity.notFound().build();
        }

        return createResponseEntity(fileName, json.getBytes(StandardCharsets.UTF_8));
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public String uploadJsonFile(UserImportExportCommand command, RedirectAttributes redirect) {
        try {
            MultipartFile myFile = command.getJsonFile();
            if (myFile == null || myFile.getBytes().length == 0) {
                messageUtil.addErrorMsg(redirect, "user.importexport.upload.msg.noFileGiven");
                return REDIRECT_SHOW_PAGE;
            }

            if (!CONTENT_TYPE_JSON.equals(myFile.getContentType())) {
                messageUtil.addErrorMsg(redirect, "user.importexport.upload.msg.noJsonFile");
                return REDIRECT_SHOW_PAGE;
            }

            int importedCount = userImportExportService.importUsers(new String(myFile.getBytes(), StandardCharsets.UTF_8));

            messageUtil.addInfoMsg(redirect, "user.importexport.upload.msg.saved",importedCount);
        } catch (IOException | ExcelReadingException e) {
            LOG.error(e.getMessage(), e);
            messageUtil.addErrorMsg(redirect, "user.importexport.upload.msg.failed", e.getMessage());
        }

        return REDIRECT_SHOW_PAGE;
    }

    private ResponseEntity<byte[]> createResponseEntity(final String fileName, byte[] fileContent) {
        return ResponseEntity.ok().header("Content-Type", CONTENT_TYPE_JSON)
                .header("Content-Disposition", "inline; filename=\"" + fileName + "\"").body(fileContent);
    }


}
