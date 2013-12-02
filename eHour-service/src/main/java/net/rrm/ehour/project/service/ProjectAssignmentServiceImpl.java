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

package net.rrm.ehour.project.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.rrm.ehour.data.DateRange;
import net.rrm.ehour.domain.Project;
import net.rrm.ehour.domain.ProjectAssignment;
import net.rrm.ehour.domain.ProjectAssignmentType;
import net.rrm.ehour.domain.User;
import net.rrm.ehour.exception.ObjectNotFoundException;
import net.rrm.ehour.persistence.project.dao.ProjectAssignmentDao;
import net.rrm.ehour.persistence.report.dao.ReportAggregatedDao;
import net.rrm.ehour.project.status.ProjectAssignmentStatusService;
import net.rrm.ehour.report.reports.element.AssignmentAggregateReportElement;
import net.rrm.ehour.report.reports.util.ReportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("projectAssignmentService")
public class ProjectAssignmentServiceImpl implements ProjectAssignmentService {
    private ProjectAssignmentDao projectAssignmentDAO;
    private ProjectAssignmentStatusService projectAssignmentStatusService;
    private ReportAggregatedDao reportAggregatedDAO;

    @Autowired
    public ProjectAssignmentServiceImpl(ProjectAssignmentDao projectAssignmentDAO, ProjectAssignmentStatusService projectAssignmentStatusService, ReportAggregatedDao reportAggregatedDAO) {
        this.projectAssignmentDAO = projectAssignmentDAO;
        this.projectAssignmentStatusService = projectAssignmentStatusService;
        this.reportAggregatedDAO = reportAggregatedDAO;
    }

    /**
     * Get active projects for user in date range
     *
     * @param userId
     * @param dateRange
     * @return
     */

    public List<ProjectAssignment> getProjectAssignmentsForUser(Integer userId, DateRange dateRange) {
        List<ProjectAssignment> assignments;
        List<ProjectAssignment> validAssignments = new ArrayList<ProjectAssignment>();

        assignments = projectAssignmentDAO.findProjectAssignmentsForUser(userId, dateRange);

        for (ProjectAssignment assignment : assignments) {
            if (projectAssignmentStatusService.getAssignmentStatus(assignment, dateRange).isAssignmentBookable()) {
                validAssignments.add(assignment);
            }
        }

        return validAssignments;
    }

    public List<ProjectAssignment> getProjectAssignmentsForUser(User user, boolean hideInactive) {
        List<ProjectAssignment> results;
        List<ProjectAssignment> filteredResults;

        results = projectAssignmentDAO.findProjectAssignmentsForUser(user);

        if (hideInactive) {
            Date today = new Date();

            filteredResults = new ArrayList<ProjectAssignment>();

            for (ProjectAssignment projectAssignment : results) {
                if (projectAssignment.isActive() &&
                        (projectAssignment.getDateStart() == null || projectAssignment.getDateStart().before(today)) &&
                        (projectAssignment.getDateEnd() == null || projectAssignment.getDateEnd().after(today))) {
                    filteredResults.add(projectAssignment);
                }
            }

            results = filteredResults;
        }

        return results;
    }


    /**
     * Get project assignment on id
     */
    public ProjectAssignment getProjectAssignment(Integer assignmentId) throws ObjectNotFoundException {
        ProjectAssignment assignment = projectAssignmentDAO.findById(assignmentId);

        if (assignment == null) {
            throw new ObjectNotFoundException("Assignment not found for id: " + assignmentId);
        }

        // call to report service needed but due to circular reference go straight to DAO
        List<AssignmentAggregateReportElement> aggregates = reportAggregatedDAO.getCumulatedHoursPerAssignmentForAssignments(Arrays.asList(assignmentId));
        assignment.setDeletable(ReportUtil.isEmptyAggregateList(aggregates));

        return assignment;
    }

    public List<ProjectAssignment> getProjectAssignments(Project project, DateRange range) {
        return projectAssignmentDAO.findProjectAssignmentsForProject(project, range);
    }

    @Override
    public List<ProjectAssignment> getProjectAssignmentsAndCheckDeletability(Project project) {
        List<ProjectAssignment> assignmentsForProject = projectAssignmentDAO.findAllProjectAssignmentsForProject(project);

        if (!assignmentsForProject.isEmpty()) {
            Map<Integer, ProjectAssignment> assignmentMap = Maps.newHashMap();

            for (ProjectAssignment projectAssignment : assignmentsForProject) {
                projectAssignment.setDeletable(true);
                assignmentMap.put(projectAssignment.getPK(), projectAssignment);
            }

            List<AssignmentAggregateReportElement> aggregates = reportAggregatedDAO.getCumulatedHoursPerAssignmentForAssignments(Lists.newArrayList(assignmentMap.keySet()));

            for (AssignmentAggregateReportElement aggregate : aggregates) {
                assignmentMap.get(aggregate.getProjectAssignment().getAssignmentId()).setDeletable(false);
            }
        }

        return assignmentsForProject;
    }

    @Override
    public List<ProjectAssignment> getActiveProjectAssignments(Project project) {
        return projectAssignmentDAO.findAllActiveProjectAssignmentsForProject(project);
    }

    public List<ProjectAssignmentType> getProjectAssignmentTypes() {
        return projectAssignmentDAO.findProjectAssignmentTypes();
    }
}
