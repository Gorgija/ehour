/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.rrm.ehour.ui.manage.user;

import net.rrm.ehour.domain.User;
import net.rrm.ehour.domain.UserDepartment;
import net.rrm.ehour.exception.ObjectNotFoundException;
import net.rrm.ehour.security.SecurityRules;
import net.rrm.ehour.sort.UserDepartmentComparator;
import net.rrm.ehour.ui.common.session.EhourWebSession;
import net.rrm.ehour.user.service.UserService;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.Collections;
import java.util.List;

/**
 * User management page using 2 tabs, an entrySelector panel and the UserForm panel
 */
public class UserManagePage extends AbstractUserManagePageTemplate<ManageUserBackingBean> {
    @SpringBean
    private UserService userService;

    private List<UserDepartment> departments;

    private static final long serialVersionUID = 1883278850247747252L;

    protected ManageUserBackingBean createEditBean(Integer userId) throws ObjectNotFoundException {
        return new ManageUserBackingBean(userService.getUserAndCheckDeletability(userId));
    }

    @Override
    protected Panel getBaseAddPanel(String panelId) {
        return new ManageUserFormPanel(panelId,
                new CompoundPropertyModel<ManageUserBackingBean>(getTabbedPanel().getAddBackingBean()),
                getUserDepartments());
    }

    @Override
    protected ManageUserBackingBean getNewAddBaseBackingBean() {
        ManageUserBackingBean userBean = new ManageUserBackingBean();
        userBean.getUser().setActive(true);

        return userBean;
    }

    @Override
    protected ManageUserBackingBean getNewEditBaseBackingBean() {
        return new ManageUserBackingBean();
    }

    @Override
    protected Panel getBaseEditPanel(String panelId) {
        ManageUserBackingBean bean = getTabbedPanel().getEditBackingBean();
        User user = bean.getUser();

        boolean editableUser = SecurityRules.allowedToModify(EhourWebSession.getUser(),
                user,
                EhourWebSession.getEhourConfig().isSplitAdminRole());

        if (editableUser) {
            return new ManageUserFormPanel(panelId,
                    new CompoundPropertyModel<ManageUserBackingBean>(bean),
                    getUserDepartments());
        } else {
            return new ManageUserReadOnlyPanel(panelId,
                    new CompoundPropertyModel<ManageUserBackingBean>(bean));
        }
    }

    private List<UserDepartment> getUserDepartments() {
        if (departments == null) {
            departments = userService.getUserDepartments();
        }

        Collections.sort(departments, new UserDepartmentComparator());

        return departments;
    }
}
